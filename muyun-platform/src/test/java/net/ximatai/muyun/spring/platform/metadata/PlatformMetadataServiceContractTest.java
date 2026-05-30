package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaClause;
import net.ximatai.muyun.database.core.orm.CriteriaGroup;
import net.ximatai.muyun.database.core.orm.CriteriaOperator;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.common.model.capability.SortCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewFieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewType;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.dynamic.metadata.ViewControlType;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategory;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryKind;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryService;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformMetadataServiceContractTest {
    private final MemoryDao<PlatformModule> moduleDao = new MemoryDao<>();
    private final MemoryDao<Metadata> metadataDao = new MemoryDao<>();
    private final MemoryDao<MetadataField> fieldDao = new MemoryDao<>();
    private final MemoryDao<PlatformFieldType> fieldTypeDao = new MemoryDao<>();
    private final MemoryDao<MetadataFieldConfig> fieldConfigDao = new MemoryDao<>();
    private final MemoryDao<MetadataFieldReferenceConfig> referenceConfigDao = new MemoryDao<>();
    private final MemoryDao<ModuleMetadataRelation> relationDao = new MemoryDao<>();
    private final MemoryDao<MetadataView> viewDao = new MemoryDao<>();
    private final MemoryDao<MetadataViewField> viewFieldDao = new MemoryDao<>();
    private final MemoryDao<DictionaryCategory> categoryDao = new MemoryDao<>();
    private final PlatformModuleService moduleService = new PlatformModuleService(moduleDao);
    private final MetadataService metadataService = new MetadataService(metadataDao);
    private final DictionaryCategoryService categoryService = new DictionaryCategoryService(categoryDao);
    private final PlatformFieldTypeService fieldTypeService = new PlatformFieldTypeService(fieldTypeDao);
    private final MetadataFieldService fieldService = new MetadataFieldService(fieldDao, metadataService, fieldTypeService);
    private final ModuleMetadataRelationService relationService =
            new ModuleMetadataRelationService(relationDao, moduleService, metadataService);
    private final MetadataFieldConfigService fieldConfigService =
            new MetadataFieldConfigService(fieldConfigDao, fieldService, metadataService, fieldTypeService,
                    categoryService, relationService);
    private final MetadataFieldDefinitionCompiler fieldDefinitionCompiler =
            new MetadataFieldDefinitionCompiler(fieldTypeService, fieldConfigService);
    private final MetadataFieldReferenceConfigService referenceConfigService =
            new MetadataFieldReferenceConfigService(referenceConfigDao, fieldService, metadataService,
                    fieldTypeService, moduleService, relationService);
    private final MetadataViewService viewService = new MetadataViewService(viewDao, relationService);
    private final MetadataViewFieldService viewFieldService =
            new MetadataViewFieldService(viewFieldDao, viewService, fieldService, relationService);

    {
        fieldTypeService.insert(fieldType("string", FieldType.STRING, 128));
        fieldTypeService.insert(fieldType("integer", FieldType.INTEGER, null));
    }

    @Test
    void shouldCreateMetadataWithApplicationScopedAliasAndPhysicalLocation() {
        Metadata metadata = metadata("crm", "customer");

        String id = metadataService.insert(metadata);

        Metadata saved = metadataService.select(id);
        assertThat(saved.getApplicationAlias()).isEqualTo("crm");
        assertThat(saved.getAlias()).isEqualTo("customer");
        assertThat(saved.getSchemaName()).isEqualTo(MetadataService.DEFAULT_SCHEMA);
        assertThat(saved.getTableName()).isEqualTo("crm_customer");
        assertThat(saved.getEnabled()).isTrue();
    }

    @Test
    void shouldRejectInvalidMetadataAlias() {
        Metadata metadata = metadata("crm", "Customer");

        assertThatThrownBy(() -> metadataService.insert(metadata))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metadataAlias");
    }

    @Test
    void shouldRejectDuplicateMetadataAliasAndPhysicalTable() {
        metadataService.insert(metadata("crm", "customer"));

        assertThatThrownBy(() -> metadataService.insert(metadata("crm", "customer")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("metadataAlias");

        Metadata duplicateTable = metadata("sales", "customer");
        duplicateTable.setSchemaName(MetadataService.DEFAULT_SCHEMA);
        duplicateTable.setTableName("crm_customer");
        assertThatThrownBy(() -> metadataService.insert(duplicateTable))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("physical table");
    }

    @Test
    void shouldCreateMetadataFieldAndCompileFieldDefinition() {
        String metadataId = metadataService.insert(metadata("crm", "customer"));
        MetadataField field = field(metadataId, "customerName", "customer_name", FieldType.STRING);
        field.setTitle("客户名称");
        field.setRequired(true);
        field.setTitleField(true);

        fieldService.insert(field);

        FieldDefinition definition = fieldDefinitionCompiler.compile(field);
        assertThat(definition.fieldName()).isEqualTo("customerName");
        assertThat(definition.columnName()).isEqualTo("customer_name");
        assertThat(definition.type()).isEqualTo(FieldType.STRING);
        assertThat(definition.isRequired()).isTrue();
        assertThat(definition.isTitle()).isTrue();
        assertThat(definition.length()).isEqualTo(128);
    }

    @Test
    void shouldCompileFieldQueryDefinition() {
        String metadataId = metadataService.insert(metadata("crm", "customer"));
        MetadataField field = field(metadataId, "customerName", "customer_name", FieldType.STRING);

        fieldService.insert(field);

        FieldDefinition definition = fieldDefinitionCompiler.compile(field);
        assertThat(definition.queryDefinition().queryable()).isTrue();
        assertThat(definition.queryDefinition().defaultOperator()).isEqualTo(DynamicQueryOperator.LIKE);
    }

    @Test
    void shouldCompileDictionaryBindingOnMetadataField() {
        String metadataId = metadataService.insert(metadata("crm", "customer"));
        categoryService.insert(category("crm", "customer_status", DictionaryCategoryKind.DICTIONARY));
        MetadataField field = field(metadataId, "status", "status", FieldType.STRING);
        fieldService.insert(field);
        MetadataFieldConfig config = fieldConfig(field.getId());
        config.setDictionaryCategoryAlias("customer_status");
        fieldConfigService.insert(config);

        FieldDefinition definition = fieldDefinitionCompiler.compile(field);
        assertThat(config.getDictionaryApplicationAlias()).isEqualTo("crm");
        assertThat(definition.dictionaryBinding().applicationAlias()).isEqualTo("crm");
        assertThat(definition.dictionaryBinding().categoryAlias()).isEqualTo("customer_status");
        assertThat(definition.queryDefinition().queryable()).isTrue();
        assertThat(definition.queryDefinition().defaultOperator()).isEqualTo(DynamicQueryOperator.LIKE);
    }

    @Test
    void shouldOverrideFieldShapeWithoutChangingFieldTypeCatalog() {
        String metadataId = metadataService.insert(metadata("crm", "customer"));
        MetadataField field = field(metadataId, "shortCode", "short_code", FieldType.STRING);
        fieldService.insert(field);
        MetadataFieldConfig config = fieldConfig(field.getId());
        config.setFieldLength(32);
        fieldConfigService.insert(config);

        FieldDefinition definition = fieldDefinitionCompiler.compile(field);
        assertThat(definition.type()).isEqualTo(FieldType.STRING);
        assertThat(definition.length()).isEqualTo(32);
        assertThat(definition.queryDefinition().queryable()).isTrue();
    }

    @Test
    void shouldOverrideFieldBehaviorInRelationScope() {
        moduleService.insert(module("crm.customer", "crm", ModuleKind.DYNAMIC));
        String metadataId = metadataService.insert(metadata("crm", "customer"));
        MetadataField field = field(metadataId, "status", "status", FieldType.STRING);
        fieldService.insert(field);
        String relationId = relationService.insert(mainRelation("crm.customer", metadataId));
        MetadataFieldConfig defaultConfig = fieldConfig(field.getId());
        defaultConfig.setFieldLength(64);
        fieldConfigService.insert(defaultConfig);
        MetadataFieldConfig override = fieldConfig(field.getId());
        override.setRelationId(relationId);
        override.setQueryable(false);
        fieldConfigService.insert(override);

        FieldDefinition defaultDefinition = fieldDefinitionCompiler.compile(field);
        FieldDefinition scopedDefinition = fieldDefinitionCompiler.compile(field, relationId);

        assertThat(defaultDefinition.queryDefinition().queryable()).isTrue();
        assertThat(scopedDefinition.queryDefinition().queryable()).isFalse();
        assertThat(scopedDefinition.length()).isEqualTo(64);
    }

    @Test
    void shouldRejectRelationScopedPhysicalShapeOverride() {
        moduleService.insert(module("crm.customer", "crm", ModuleKind.DYNAMIC));
        String metadataId = metadataService.insert(metadata("crm", "customer"));
        MetadataField field = field(metadataId, "shortCode", "short_code", FieldType.STRING);
        fieldService.insert(field);
        String relationId = relationService.insert(mainRelation("crm.customer", metadataId));
        MetadataFieldConfig config = fieldConfig(field.getId());
        config.setRelationId(relationId);
        config.setFieldLength(32);

        assertThatThrownBy(() -> fieldConfigService.insert(config))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("physical field shape");
    }

    @Test
    void shouldRejectFieldShapeThatDoesNotMatchFieldType() {
        PlatformFieldType invalidType = fieldType("integer_length", FieldType.INTEGER, 32);
        assertThatThrownBy(() -> fieldTypeService.insert(invalidType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("length only applies");

        String metadataId = metadataService.insert(metadata("crm", "customer"));
        MetadataField field = field(metadataId, "amount", "amount", FieldType.INTEGER);
        fieldService.insert(field);
        MetadataFieldConfig config = fieldConfig(field.getId());
        config.setFieldLength(32);

        assertThatThrownBy(() -> fieldConfigService.insert(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("length only applies");
    }

    @Test
    void shouldRejectDictionaryBindingOnNonStringField() {
        String metadataId = metadataService.insert(metadata("crm", "customer"));
        categoryService.insert(category("crm", "customer_status", DictionaryCategoryKind.DICTIONARY));
        MetadataField field = field(metadataId, "status", "status", FieldType.INTEGER);
        fieldService.insert(field);
        MetadataFieldConfig config = fieldConfig(field.getId());
        config.setDictionaryCategoryAlias("customer_status");

        assertThatThrownBy(() -> fieldConfigService.insert(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dictionary binding");
    }

    @Test
    void shouldRejectDictionaryBindingWithoutExistingCategory() {
        String metadataId = metadataService.insert(metadata("crm", "customer"));
        MetadataField field = field(metadataId, "status", "status", FieldType.STRING);
        fieldService.insert(field);
        MetadataFieldConfig config = fieldConfig(field.getId());
        config.setDictionaryCategoryAlias("customer_status");

        assertThatThrownBy(() -> fieldConfigService.insert(config))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("existing category");
    }

    @Test
    void shouldCreateMetadataFieldReferenceConfig() {
        moduleService.insert(module("crm.customer", "crm", ModuleKind.DYNAMIC));
        String customerId = metadataService.insert(metadata("crm", "customer"));
        String contactId = metadataService.insert(metadata("crm", "contact"));
        fieldService.insert(titleField(customerId));
        fieldService.insert(field(customerId, "code", "code", FieldType.STRING));
        MetadataField customerField = field(contactId, "customerId", "customer_id", FieldType.STRING);
        fieldService.insert(customerField);
        MetadataFieldReferenceConfig config = referenceConfig(customerField.getId(), customerId);
        config.setAutoTitle(true);
        config.setProjectionMappings("code:customerCode");

        String id = referenceConfigService.insert(config);

        MetadataFieldReferenceConfig saved = referenceConfigService.select(id);
        assertThat(saved.getCardinality()).isEqualTo(net.ximatai.muyun.spring.ability.reference.ReferenceCardinality.ONE);
        assertThat(saved.getTitleOutputField()).isEqualTo("customerIdTitle");
        assertThat(saved.projections()).hasSize(1);
    }

    @Test
    void shouldRejectReferenceConfigForNonStringSourceField() {
        String customerId = metadataService.insert(metadata("crm", "customer"));
        String contactId = metadataService.insert(metadata("crm", "contact"));
        MetadataField customerField = field(contactId, "customerId", "customer_id", FieldType.INTEGER);
        fieldService.insert(customerField);
        MetadataFieldReferenceConfig config = referenceConfig(customerField.getId(), customerId);

        assertThatThrownBy(() -> referenceConfigService.insert(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source field must be string");
    }

    @Test
    void shouldRejectCrossModuleReferenceDisplayConfig() {
        moduleService.insert(module("crm.customer", "crm", ModuleKind.DYNAMIC));
        String customerId = metadataService.insert(metadata("crm", "customer"));
        String contactId = metadataService.insert(metadata("crm", "contact"));
        MetadataField customerField = field(contactId, "customerId", "customer_id", FieldType.STRING);
        fieldService.insert(customerField);
        MetadataFieldReferenceConfig config = referenceConfig(customerField.getId(), customerId);
        config.setTargetModuleAlias("crm.customer");
        config.setAutoTitle(true);

        assertThatThrownBy(() -> referenceConfigService.insert(config))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("Cross-module reference display");
    }

    @Test
    void shouldCreateRelationScopedMetadataViewFields() {
        moduleService.insert(module("crm.customer", "crm", ModuleKind.DYNAMIC));
        String metadataId = metadataService.insert(metadata("crm", "customer"));
        fieldService.insert(titleField(metadataId));
        MetadataField status = field(metadataId, "status", "status", FieldType.STRING);
        fieldService.insert(status);
        String relationId = relationService.insert(mainRelation("crm.customer", metadataId));
        MetadataView view = metadataView(relationId, EntityViewType.LIST);
        String viewId = viewService.insert(view);
        MetadataViewField viewField = metadataViewField(viewId, status.getId());
        viewField.setControlType(ViewControlType.SELECT);
        viewField.setReadOnly(true);
        viewField.setRequiredOverride(true);

        viewFieldService.insert(viewField);

        EntityViewFieldDefinition definition = viewFieldService.compile(viewField);
        assertThat(viewService.listByRelationIds(List.of(relationId))).extracting(MetadataView::getId)
                .containsExactly(viewId);
        assertThat(definition.fieldName()).isEqualTo("status");
        assertThat(definition.controlType()).isEqualTo(ViewControlType.SELECT);
        assertThat(definition.readOnly()).isTrue();
        assertThat(definition.required()).isTrue();
    }

    @Test
    void shouldRejectViewFieldOutsideRelationMetadata() {
        moduleService.insert(module("crm.customer", "crm", ModuleKind.DYNAMIC));
        String customerId = metadataService.insert(metadata("crm", "customer"));
        String contactId = metadataService.insert(metadata("crm", "contact"));
        fieldService.insert(titleField(customerId));
        MetadataField contactTitle = titleField(contactId);
        fieldService.insert(contactTitle);
        String relationId = relationService.insert(mainRelation("crm.customer", customerId));
        String viewId = viewService.insert(metadataView(relationId, EntityViewType.FORM));

        assertThatThrownBy(() -> viewFieldService.insert(metadataViewField(viewId, contactTitle.getId())))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("metadata mismatch");
    }

    @Test
    void shouldRejectViewFieldThatRelaxesRequiredMetadataField() {
        moduleService.insert(module("crm.customer", "crm", ModuleKind.DYNAMIC));
        String metadataId = metadataService.insert(metadata("crm", "customer"));
        MetadataField code = field(metadataId, "code", "code", FieldType.STRING);
        code.setRequired(true);
        fieldService.insert(code);
        String relationId = relationService.insert(mainRelation("crm.customer", metadataId));
        String viewId = viewService.insert(metadataView(relationId, EntityViewType.FORM));
        MetadataViewField viewField = metadataViewField(viewId, code.getId());
        viewField.setRequiredOverride(false);

        assertThatThrownBy(() -> viewFieldService.insert(viewField))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("cannot make required");
    }

    @Test
    void shouldRejectFieldWithoutExistingMetadata() {
        MetadataField field = field("missing", "code", "code", FieldType.STRING);

        assertThatThrownBy(() -> fieldService.insert(field))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("existing metadata");
    }

    @Test
    void shouldRejectDuplicateFieldNameColumnNameAndSingleTitleField() {
        String metadataId = metadataService.insert(metadata("crm", "customer"));
        MetadataField code = field(metadataId, "code", "code", FieldType.STRING);
        code.setTitleField(true);
        fieldService.insert(code);

        assertThatThrownBy(() -> fieldService.insert(field(metadataId, "code", "customer_code", FieldType.STRING)))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("fieldName");
        assertThatThrownBy(() -> fieldService.insert(field(metadataId, "customerCode", "code", FieldType.STRING)))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("columnName");

        MetadataField name = field(metadataId, "name", "name", FieldType.STRING);
        name.setTitleField(true);
        assertThatThrownBy(() -> fieldService.insert(name))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("title field");
    }

    @Test
    void shouldBindMainMetadataAndRejectDuplicateMainRelation() {
        moduleService.insert(module("crm.customer", "crm", ModuleKind.DYNAMIC));
        String customerMetadataId = metadataService.insert(metadata("crm", "customer"));
        String profileMetadataId = metadataService.insert(metadata("crm", "profile"));

        relationService.insert(mainRelation("crm.customer", customerMetadataId));

        assertThatThrownBy(() -> relationService.insert(mainRelation("crm.customer", profileMetadataId)))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("one MAIN");
    }

    @Test
    void shouldRejectDuplicateRelationAliasWithinModule() {
        moduleService.insert(module("crm.customer", "crm", ModuleKind.DYNAMIC));
        String customerMetadataId = metadataService.insert(metadata("crm", "customer"));
        String profileMetadataId = metadataService.insert(metadata("crm", "profile"));
        String noteMetadataId = metadataService.insert(metadata("crm", "note"));
        relationService.insert(mainRelation("crm.customer", customerMetadataId));
        relationService.insert(childRelation("crm.customer", profileMetadataId, customerMetadataId));
        ModuleMetadataRelation duplicateAlias = childRelation("crm.customer", noteMetadataId, customerMetadataId);

        assertThatThrownBy(() -> relationService.insert(duplicateAlias))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("relationAlias");
    }

    @Test
    void shouldRequireMainRelationBeforeDynamicChildRelation() {
        moduleService.insert(module("crm.customer", "crm", ModuleKind.DYNAMIC));
        String customerMetadataId = metadataService.insert(metadata("crm", "customer"));
        String profileMetadataId = metadataService.insert(metadata("crm", "profile"));
        ModuleMetadataRelation child = childRelation("crm.customer", profileMetadataId, customerMetadataId);

        assertThatThrownBy(() -> relationService.insert(child))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("parent metadata relation");
    }

    @Test
    void shouldBindChildRelationAfterMainRelation() {
        moduleService.insert(module("crm.customer", "crm", ModuleKind.DYNAMIC));
        String customerMetadataId = metadataService.insert(metadata("crm", "customer"));
        String profileMetadataId = metadataService.insert(metadata("crm", "profile"));
        relationService.insert(mainRelation("crm.customer", customerMetadataId));
        ModuleMetadataRelation child = childRelation("crm.customer", profileMetadataId, customerMetadataId);

        relationService.insert(child);

        assertThat(relationService.list(Criteria.of().eq("moduleAlias", "crm.customer"), PageRequest.of(1, 10)))
                .extracting(ModuleMetadataRelation::getRelationRole)
                .containsExactly(RelationRole.MAIN, RelationRole.CHILD);
    }

    private Metadata metadata(String applicationAlias, String alias) {
        Metadata metadata = new Metadata();
        metadata.setApplicationAlias(applicationAlias);
        metadata.setAlias(alias);
        metadata.setTitle(alias);
        return metadata;
    }

    private MetadataField field(String metadataId, String fieldName, String columnName, FieldType fieldType) {
        MetadataField field = new MetadataField();
        field.setMetadataId(metadataId);
        field.setFieldName(fieldName);
        field.setColumnName(columnName);
        field.setFieldTypeAlias(fieldType.name().toLowerCase());
        field.setTitle(fieldName);
        return field;
    }

    private MetadataField titleField(String metadataId) {
        MetadataField field = field(metadataId, "title", "title", FieldType.STRING);
        field.setTitleField(true);
        return field;
    }

    private PlatformFieldType fieldType(String alias, FieldType fieldType, Integer length) {
        PlatformFieldType type = new PlatformFieldType();
        type.setAlias(alias);
        type.setTitle(alias);
        type.setFieldType(fieldType);
        type.setDefaultLength(length);
        type.setDefaultQueryOperator(DynamicQueryOperator.defaultOperator(fieldType));
        type.setQueryOperators(DynamicQueryOperator.format(DynamicQueryOperator.defaultOperators(fieldType)));
        return type;
    }

    private MetadataFieldConfig fieldConfig(String fieldId) {
        MetadataFieldConfig config = new MetadataFieldConfig();
        config.setMetadataFieldId(fieldId);
        return config;
    }

    private MetadataFieldReferenceConfig referenceConfig(String fieldId, String targetMetadataId) {
        MetadataFieldReferenceConfig config = new MetadataFieldReferenceConfig();
        config.setMetadataFieldId(fieldId);
        config.setTargetMetadataId(targetMetadataId);
        return config;
    }

    private MetadataView metadataView(String relationId, EntityViewType viewType) {
        MetadataView view = new MetadataView();
        view.setRelationId(relationId);
        view.setViewType(viewType);
        return view;
    }

    private MetadataViewField metadataViewField(String viewId, String fieldId) {
        MetadataViewField viewField = new MetadataViewField();
        viewField.setViewId(viewId);
        viewField.setMetadataFieldId(fieldId);
        return viewField;
    }

    private DictionaryCategory category(String applicationAlias, String alias, DictionaryCategoryKind kind) {
        DictionaryCategory category = new DictionaryCategory();
        category.setApplicationAlias(applicationAlias);
        category.setAlias(alias);
        category.setCategoryKind(kind);
        category.setTitle(alias);
        return category;
    }

    private PlatformModule module(String alias, String applicationAlias, ModuleKind kind) {
        PlatformModule module = new PlatformModule();
        module.setAlias(alias);
        module.setApplicationAlias(applicationAlias);
        module.setModuleKind(kind);
        module.setTitle(alias);
        return module;
    }

    private ModuleMetadataRelation mainRelation(String moduleAlias, String metadataId) {
        ModuleMetadataRelation relation = new ModuleMetadataRelation();
        relation.setModuleAlias(moduleAlias);
        relation.setMetadataId(metadataId);
        relation.setRelationRole(RelationRole.MAIN);
        relation.setTitle("main");
        return relation;
    }

    private ModuleMetadataRelation childRelation(String moduleAlias, String metadataId, String parentMetadataId) {
        ModuleMetadataRelation relation = new ModuleMetadataRelation();
        relation.setModuleAlias(moduleAlias);
        relation.setMetadataId(metadataId);
        relation.setParentMetadataId(parentMetadataId);
        relation.setRelationRole(RelationRole.CHILD);
        relation.setForeignKey("customerId");
        relation.setRelationAlias("profile");
        relation.setTitle("profile");
        return relation;
    }

    private static class MemoryDao<T extends EntityContract> implements BaseDao<T, String> {
        private final Map<String, T> rows = new LinkedHashMap<>();

        @Override
        public boolean ensureTable() {
            return true;
        }

        @Override
        public String insert(T entity) {
            rows.put(entity.getId(), entity);
            return entity.getId();
        }

        @Override
        public int updateById(T entity) {
            rows.put(entity.getId(), entity);
            return 1;
        }

        @Override
        public int updateByIdAndCondition(T entity, Map<String, Object> conditions) {
            rows.put(entity.getId(), entity);
            return 1;
        }

        @Override
        public int deleteById(String id) {
            return rows.remove(id) == null ? 0 : 1;
        }

        @Override
        public int deleteByIdAndCondition(String id, Map<String, Object> conditions) {
            return deleteById(id);
        }

        @Override
        public boolean existsById(String id) {
            return rows.containsKey(id);
        }

        @Override
        public T findById(String id) {
            return rows.get(id);
        }

        @Override
        public List<T> query(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
            List<T> filtered = rows.values().stream()
                    .filter(row -> matches(row, criteria))
                    .sorted(Comparator.comparing(this::sortOrder, Comparator.nullsLast(Integer::compareTo)))
                    .toList();
            int from = Math.min(pageRequest.getOffset(), filtered.size());
            int to = Math.min(from + pageRequest.getLimit(), filtered.size());
            return new ArrayList<>(filtered.subList(from, to));
        }

        @Override
        public PageResult<T> pageQuery(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
            List<T> records = query(criteria, pageRequest, sorts);
            return PageResult.of(records, records.size(), pageRequest);
        }

        @Override
        public long count(Criteria criteria) {
            return rows.values().stream().filter(row -> matches(row, criteria)).count();
        }

        @Override
        public int upsert(T entity) {
            rows.put(entity.getId(), entity);
            return 1;
        }

        private Integer sortOrder(T row) {
            return row instanceof SortCapable sortable ? sortable.getSortOrder() : null;
        }

        private boolean matches(T row, Criteria criteria) {
            if (criteria == null || criteria.isEmpty()) {
                return true;
            }
            return matchesGroup(row, criteria.getRoot());
        }

        private boolean matchesGroup(T row, CriteriaGroup group) {
            Boolean matched = null;
            for (CriteriaGroup.Entry entry : group.getEntries()) {
                boolean entryMatched = matchesNode(row, entry.getNode());
                if (matched == null) {
                    matched = entryMatched;
                } else if (isOrJoin(entry)) {
                    matched = matched || entryMatched;
                } else {
                    matched = matched && entryMatched;
                }
            }
            return matched == null || matched;
        }

        private boolean matchesNode(T row, Object node) {
            if (node instanceof CriteriaClause clause) {
                return matchesClause(row, clause);
            }
            if (node instanceof CriteriaGroup group) {
                return matchesGroup(row, group);
            }
            return true;
        }

        private boolean matchesClause(T row, CriteriaClause clause) {
            Object actual = value(row, clause.getField());
            if (clause.getOperator() == CriteriaOperator.IS_NULL) {
                return actual == null;
            }
            if (clause.getOperator() == CriteriaOperator.IS_NOT_NULL) {
                return actual != null;
            }
            if (clause.getOperator() == CriteriaOperator.EQ) {
                Object expected = clause.getValues().getFirst();
                return expected == null ? actual == null : expected.equals(actual);
            }
            return true;
        }

        private Object value(T row, String field) {
            try {
                String getter = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
                return row.getClass().getMethod(getter).invoke(row);
            } catch (ReflectiveOperationException e) {
                try {
                    String getter = "is" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
                    return row.getClass().getMethod(getter).invoke(row);
                } catch (ReflectiveOperationException ignored) {
                    return null;
                }
            }
        }

        private boolean isOrJoin(CriteriaGroup.Entry entry) {
            try {
                Method method = entry.getClass().getMethod("getJoin");
                return "OR".equals(String.valueOf(method.invoke(entry)));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot read criteria join", e);
            }
        }
    }
}
