package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaClause;
import net.ximatai.muyun.database.core.orm.CriteriaGroup;
import net.ximatai.muyun.database.core.orm.CriteriaOperator;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.platform.metadata.Metadata;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.MetadataService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataField;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldType;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldTypeService;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiType;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeService;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformUiConfigurationServiceContractTest {
    private final TestMemoryDao<PlatformModule> moduleDao = new TestMemoryDao<>();
    private final TestMemoryDao<Metadata> metadataDao = new TestMemoryDao<>();
    private final TestMemoryDao<MetadataField> fieldDao = new TestMemoryDao<>();
    private final TestMemoryDao<ModuleMetadataRelation> relationDao = new TestMemoryDao<>();
    private final TestMemoryDao<ModuleMetadataField> moduleFieldDao = new TestMemoryDao<>();
    private final TestMemoryDao<PlatformFieldType> fieldTypeDao = new TestMemoryDao<>();
    private final TestMemoryDao<PlatformFieldUiType> fieldUiTypeDao = new TestMemoryDao<>();
    private final TestMemoryDao<PlatformUiSet> uiSetDao = new TestMemoryDao<>();
    private final TestMemoryDao<PlatformUiConfig> uiConfigDao = new TestMemoryDao<>();
    private final TestMemoryDao<PlatformUiConfigField> uiConfigFieldDao = new TestMemoryDao<>();
    private final TestMemoryDao<PlatformQueryTemplate> queryTemplateDao = new TestMemoryDao<>();
    private final TestMemoryDao<PlatformQueryItem> queryItemDao = new TestMemoryDao<>();

    private final PlatformModuleService moduleService = new PlatformModuleService(moduleDao);
    private final MetadataService metadataService = new MetadataService(metadataDao);
    private final PlatformFieldTypeService fieldTypeService = new PlatformFieldTypeService(fieldTypeDao, fieldUiTypeDao);
    private final PlatformFieldUiTypeService fieldUiTypeService =
            new PlatformFieldUiTypeService(fieldUiTypeDao, fieldTypeService);
    private final MetadataFieldService fieldService = new MetadataFieldService(fieldDao, metadataService, fieldTypeService);
    private final ModuleMetadataRelationService relationService =
            new ModuleMetadataRelationService(relationDao, moduleService, metadataService);
    private final ModuleMetadataFieldService moduleFieldService =
            new ModuleMetadataFieldService(moduleFieldDao, relationService, metadataService, fieldService);
    private final PlatformUiSetService uiSetService = new PlatformUiSetService(uiSetDao, moduleService);
    private final PlatformUiConfigService uiConfigService = new PlatformUiConfigService(uiConfigDao, uiSetService);
    private final PlatformUiConfigFieldService uiConfigFieldService = new PlatformUiConfigFieldService(
            uiConfigFieldDao, uiConfigService, uiSetService, moduleFieldService, fieldTypeService, fieldUiTypeService,
            fieldService);
    private final PlatformQueryTemplateService queryTemplateService =
            new PlatformQueryTemplateService(queryTemplateDao, moduleService);
    private final PlatformQueryItemService queryItemService =
            new PlatformQueryItemService(queryItemDao, queryTemplateService, moduleFieldService, fieldTypeService);
    private final PlatformPageConfigSnapshotService snapshotService = new PlatformPageConfigSnapshotService(
            uiSetService, uiConfigService, uiConfigFieldService, queryTemplateService, queryItemService);

    @Test
    void shouldCreateUiConfigWithFieldsAndSnapshotByModule() {
        seedFieldType("string", FieldType.STRING, DynamicQueryOperator.LIKE);
        seedUiType("text", "string");
        String customerNameField = seedModuleField("crm.customer", "customer", "customerName", "customer_name", "string");

        String uiSetId = uiSetService.insert(uiSet("crm.customer", "list", PlatformUiSetType.LIST, true));
        String uiConfigId = uiConfigService.insert(uiConfig(uiSetId, PlatformUiClientType.WEB, true));
        String appDraftConfigId = uiConfigService.insert(uiConfig(uiSetId, PlatformUiClientType.APP, false));
        PlatformUiConfigField field = uiField(uiConfigId, customerNameField, "text");
        field.setWidth(180);
        uiConfigFieldService.insert(uiField(appDraftConfigId, customerNameField, "text"));

        uiConfigFieldService.insert(field);

        PlatformPageConfigSnapshot snapshot = snapshotService.snapshot("crm.customer");
        assertThat(snapshot.moduleAlias()).isEqualTo("crm.customer");
        assertThat(snapshot.uiSets()).extracting(PlatformUiSet::getAlias).containsExactly("list");
        assertThat(snapshot.uiConfigs()).extracting(PlatformUiConfig::getUiSetId).containsExactly(uiSetId);
        assertThat(snapshot.uiConfigs()).extracting(PlatformUiConfig::getPublished).containsExactly(Boolean.TRUE);
        assertThat(snapshot.uiFields())
                .extracting(PlatformUiConfigField::getModuleMetadataFieldId)
                .containsExactly(customerNameField);
        assertThat(snapshot.uiFields().getFirst().getFieldUiTypeAlias()).isEqualTo("text");
    }

    @Test
    void shouldRejectUiFieldFromAnotherModule() {
        seedFieldType("string", FieldType.STRING, DynamicQueryOperator.LIKE);
        seedUiType("text", "string");
        seedModuleField("crm.customer", "customer", "customerName", "customer_name", "string");
        String leadNameField = seedModuleField("crm.lead", "lead", "leadName", "lead_name", "string");
        String uiSetId = uiSetService.insert(uiSet("crm.customer", "form", PlatformUiSetType.FORM, true));
        String uiConfigId = uiConfigService.insert(uiConfig(uiSetId, PlatformUiClientType.WEB, true));

        assertThatThrownBy(() -> uiConfigFieldService.insert(uiField(uiConfigId, leadNameField, "text")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("same module");
    }

    @Test
    void shouldRejectUnsupportedUiTypeAndDuplicateDefaults() {
        seedFieldType("string", FieldType.STRING, DynamicQueryOperator.LIKE);
        seedFieldType("decimal", FieldType.DECIMAL, DynamicQueryOperator.EQ);
        seedUiType("number", "decimal");
        String customerNameField = seedModuleField("crm.customer", "customer", "customerName", "customer_name", "string");
        String uiSetId = uiSetService.insert(uiSet("crm.customer", "list", PlatformUiSetType.LIST, true));
        String uiConfigId = uiConfigService.insert(uiConfig(uiSetId, PlatformUiClientType.WEB, true));

        assertThatThrownBy(() -> uiConfigFieldService.insert(uiField(uiConfigId, customerNameField, "number")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("mismatch");
        assertThatThrownBy(() -> uiSetService.insert(uiSet("crm.customer", "list2", PlatformUiSetType.LIST, true)))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("Only one default UI set");
    }

    @Test
    void shouldRejectMissingDefaultUiTypeAndRequiredWeakening() {
        seedFieldType("string", FieldType.STRING, DynamicQueryOperator.LIKE);
        seedUiType("text", "string");
        String customerNameField = seedModuleField(
                "crm.customer", "customer", "customerName", "customer_name", "string", true);
        String uiSetId = uiSetService.insert(uiSet("crm.customer", "form", PlatformUiSetType.FORM, true));
        String uiConfigId = uiConfigService.insert(uiConfig(uiSetId, PlatformUiClientType.WEB, true));

        assertThatThrownBy(() -> uiConfigFieldService.insert(uiField(uiConfigId, customerNameField, null)))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("no default UI type");

        PlatformUiConfigField weakRequired = uiField(uiConfigId, customerNameField, "text");
        weakRequired.setRequiredOverride(false);
        assertThatThrownBy(() -> uiConfigFieldService.insert(weakRequired))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("cannot weaken required");
    }

    @Test
    void shouldCreateQueryTemplateAndCompileCriteria() {
        seedFieldType("string", FieldType.STRING, DynamicQueryOperator.LIKE);
        seedUiType("text", "string");
        String customerNameField = seedModuleField("crm.customer", "customer", "customerName", "customer_name", "string");
        String templateId = queryTemplateService.insert(queryTemplate("crm.customer", "default", true));
        String groupId = queryItemService.insert(queryGroup(templateId, TreeAbility.ROOT_ID, PlatformQueryGroupOperator.AND));
        PlatformQueryItem item = queryLeaf(templateId, groupId, customerNameField, DynamicQueryOperator.LIKE);
        item.setAllowExternalValue(true);
        item.setExternalValueKey("keyword");
        queryItemService.insert(item);

        Criteria criteria = queryItemService.compile(templateId, Map.of("keyword", "acme"));

        List<CriteriaClause> clauses = clauses(criteria);
        assertThat(clauses).hasSize(1);
        assertThat(clauses.getFirst().getField()).isEqualTo("customerName");
        assertThat(clauses.getFirst().getOperator()).isEqualTo(CriteriaOperator.LIKE);
        assertThat(clauses.getFirst().getValues()).containsExactly("acme");
    }

    @Test
    void shouldCompileGroupedQueryOperatorsAndSkipDisabledItems() {
        seedFieldType("string", FieldType.STRING, DynamicQueryOperator.LIKE, DynamicQueryOperator.IN);
        seedFieldType("integer", FieldType.INTEGER, DynamicQueryOperator.EQ, DynamicQueryOperator.BETWEEN);
        seedUiType("text", "string");
        seedUiType("number", "integer");
        String nameField = seedModuleField("crm.customer", "customer", "customerName", "customer_name", "string");
        String levelField = addModuleField("crm.customer", "level", "level", "integer");
        String templateId = queryTemplateService.insert(queryTemplate("crm.customer", "advanced", true));
        String groupId = queryItemService.insert(queryGroup(templateId, TreeAbility.ROOT_ID, PlatformQueryGroupOperator.OR));
        PlatformQueryItem nameItem = queryLeaf(templateId, groupId, nameField, DynamicQueryOperator.IN);
        nameItem.setDefaultValue("alice,bob");
        queryItemService.insert(nameItem);
        PlatformQueryItem levelItem = queryLeaf(templateId, groupId, levelField, DynamicQueryOperator.BETWEEN);
        levelItem.setDefaultValue("1,10");
        queryItemService.insert(levelItem);
        String disabledId = queryItemService.insert(queryLeaf(templateId, groupId, nameField, DynamicQueryOperator.LIKE));
        queryItemService.disable(disabledId);

        Criteria criteria = queryItemService.compile(templateId);

        List<CriteriaClause> clauses = clauses(criteria);
        assertThat(clauses).extracting(CriteriaClause::getOperator)
                .containsExactlyInAnyOrder(CriteriaOperator.IN, CriteriaOperator.BETWEEN);
    }

    @Test
    void shouldRejectQueryItemCyclesAndDetachedTrees() {
        seedFieldType("string", FieldType.STRING, DynamicQueryOperator.LIKE);
        seedUiType("text", "string");
        String customerNameField = seedModuleField("crm.customer", "customer", "customerName", "customer_name", "string");
        String templateId = queryTemplateService.insert(queryTemplate("crm.customer", "default", true));
        String parentId = queryItemService.insert(queryGroup(templateId, TreeAbility.ROOT_ID, PlatformQueryGroupOperator.AND));
        String childId = queryItemService.insert(queryGroup(templateId, parentId, PlatformQueryGroupOperator.AND));
        queryItemService.insert(queryLeaf(templateId, childId, customerNameField, DynamicQueryOperator.LIKE));

        PlatformQueryItem parent = queryItemService.select(parentId);
        parent.setParentId(childId);
        assertThatThrownBy(() -> queryItemService.update(parent))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("descendant");

        PlatformQueryItem detached = queryItemService.select(childId);
        detached.setParentId("missing-parent");
        queryItemDao.updateById(detached);
        assertThatThrownBy(() -> queryItemService.compile(templateId))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("outside root tree");
    }

    @Test
    void shouldRejectUnsupportedQueryOperator() {
        seedFieldType("string", FieldType.STRING, DynamicQueryOperator.LIKE);
        seedUiType("text", "string");
        String customerNameField = seedModuleField("crm.customer", "customer", "customerName", "customer_name", "string");
        String templateId = queryTemplateService.insert(queryTemplate("crm.customer", "default", true));

        assertThatThrownBy(() -> queryItemService.insert(
                queryLeaf(templateId, TreeAbility.ROOT_ID, customerNameField, DynamicQueryOperator.BETWEEN)))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("operator is not allowed");
    }

    private void seedFieldType(String alias, FieldType type, DynamicQueryOperator defaultOperator,
                               DynamicQueryOperator... extraOperators) {
        PlatformFieldType fieldType = new PlatformFieldType();
        fieldType.setAlias(alias);
        fieldType.setTitle(alias);
        fieldType.setFieldType(type);
        fieldType.setDefaultQueryOperator(defaultOperator);
        Set<String> operators = new java.util.LinkedHashSet<>();
        operators.add(defaultOperator.name());
        for (DynamicQueryOperator operator : extraOperators) {
            operators.add(operator.name());
        }
        fieldType.setQueryOperators(operators);
        fieldTypeService.insert(fieldType);
    }

    private void seedUiType(String alias, String defaultFieldTypeAlias) {
        PlatformFieldUiType uiType = new PlatformFieldUiType();
        uiType.setAlias(alias);
        uiType.setTitle(alias);
        uiType.setDefaultFieldTypeAlias(defaultFieldTypeAlias);
        fieldUiTypeService.insert(uiType);
    }

    private String seedModuleField(String moduleAlias,
                                   String metadataAlias,
                                   String fieldName,
                                   String columnName,
                                   String fieldTypeAlias) {
        return seedModuleField(moduleAlias, metadataAlias, fieldName, columnName, fieldTypeAlias, false);
    }

    private String seedModuleField(String moduleAlias,
                                   String metadataAlias,
                                   String fieldName,
                                   String columnName,
                                   String fieldTypeAlias,
                                   boolean required) {
        String applicationAlias = moduleAlias.substring(0, moduleAlias.indexOf('.'));
        PlatformModule module = new PlatformModule();
        module.setApplicationAlias(applicationAlias);
        module.setAlias(moduleAlias);
        module.setTitle(moduleAlias);
        module.setParentId(TreeAbility.ROOT_ID);
        module.setModuleKind(ModuleKind.DYNAMIC);
        moduleService.insert(module);

        Metadata metadata = new Metadata();
        metadata.setApplicationAlias(applicationAlias);
        metadata.setAlias(metadataAlias);
        metadata.setTitle(metadataAlias);
        String metadataId = metadataService.insert(metadata);

        MetadataField field = new MetadataField();
        field.setMetadataId(metadataId);
        field.setFieldName(fieldName);
        field.setColumnName(columnName);
        field.setFieldTypeAlias(fieldTypeAlias);
        field.setTitle(fieldName);
        field.setRequired(required);
        String fieldId = fieldService.insert(field);

        ModuleMetadataRelation relation = new ModuleMetadataRelation();
        relation.setModuleAlias(moduleAlias);
        relation.setMetadataId(metadataId);
        relation.setRelationAlias(metadataAlias);
        String relationId = relationService.insert(relation);

        ModuleMetadataField moduleField = new ModuleMetadataField();
        moduleField.setRelationId(relationId);
        moduleField.setMetadataFieldId(fieldId);
        moduleField.setTitle(fieldName);
        return moduleFieldService.insert(moduleField);
    }

    private String addModuleField(String moduleAlias, String fieldName, String columnName, String fieldTypeAlias) {
        ModuleMetadataRelation relation = relationDao
                .query(Criteria.of().eq("moduleAlias", moduleAlias), new PageRequest(0, 1))
                .getFirst();
        MetadataField field = new MetadataField();
        field.setMetadataId(relation.getMetadataId());
        field.setFieldName(fieldName);
        field.setColumnName(columnName);
        field.setFieldTypeAlias(fieldTypeAlias);
        field.setTitle(fieldName);
        String fieldId = fieldService.insert(field);

        ModuleMetadataField moduleField = new ModuleMetadataField();
        moduleField.setRelationId(relation.getId());
        moduleField.setMetadataFieldId(fieldId);
        moduleField.setTitle(fieldName);
        return moduleFieldService.insert(moduleField);
    }

    private PlatformUiSet uiSet(String moduleAlias, String alias, PlatformUiSetType setType, boolean defaultSet) {
        PlatformUiSet uiSet = new PlatformUiSet();
        uiSet.setModuleAlias(moduleAlias);
        uiSet.setAlias(alias);
        uiSet.setSetType(setType);
        uiSet.setDefaultSet(defaultSet);
        return uiSet;
    }

    private PlatformUiConfig uiConfig(String uiSetId, PlatformUiClientType clientType, boolean published) {
        PlatformUiConfig uiConfig = new PlatformUiConfig();
        uiConfig.setUiSetId(uiSetId);
        uiConfig.setClientType(clientType);
        uiConfig.setPublished(published);
        return uiConfig;
    }

    private PlatformUiConfigField uiField(String uiConfigId, String moduleMetadataFieldId, String fieldUiTypeAlias) {
        PlatformUiConfigField field = new PlatformUiConfigField();
        field.setUiConfigId(uiConfigId);
        field.setModuleMetadataFieldId(moduleMetadataFieldId);
        field.setFieldUiTypeAlias(fieldUiTypeAlias);
        return field;
    }

    private PlatformQueryTemplate queryTemplate(String moduleAlias, String alias, boolean defaultTemplate) {
        PlatformQueryTemplate template = new PlatformQueryTemplate();
        template.setModuleAlias(moduleAlias);
        template.setAlias(alias);
        template.setDefaultTemplate(defaultTemplate);
        return template;
    }

    private PlatformQueryItem queryGroup(String templateId, String parentId, PlatformQueryGroupOperator groupOperator) {
        PlatformQueryItem item = new PlatformQueryItem();
        item.setQueryTemplateId(templateId);
        item.setParentId(parentId);
        item.setGroupOperator(groupOperator);
        return item;
    }

    private PlatformQueryItem queryLeaf(String templateId,
                                        String parentId,
                                        String moduleMetadataFieldId,
                                        DynamicQueryOperator operator) {
        PlatformQueryItem item = new PlatformQueryItem();
        item.setQueryTemplateId(templateId);
        item.setParentId(parentId);
        item.setModuleMetadataFieldId(moduleMetadataFieldId);
        item.setOperator(operator);
        item.setDefaultValue("default");
        return item;
    }

    private List<CriteriaClause> clauses(Criteria criteria) {
        List<CriteriaClause> result = new ArrayList<>();
        collect(criteria.getRoot(), result);
        return result;
    }

    private void collect(CriteriaGroup group, List<CriteriaClause> result) {
        for (CriteriaGroup.Entry entry : group.getEntries()) {
            Object node = node(entry);
            if (node instanceof CriteriaClause clause) {
                result.add(clause);
            } else if (node instanceof CriteriaGroup childGroup) {
                collect(childGroup, result);
            }
        }
    }

    private Object node(CriteriaGroup.Entry entry) {
        try {
            Method method = entry.getClass().getMethod("getNode");
            return method.invoke(entry);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read criteria node", e);
        }
    }
}
