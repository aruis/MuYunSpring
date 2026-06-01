package net.ximatai.muyun.spring.platform.publish;

import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.AssociationViewDisplayMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionKind;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityAssociationViewDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewType;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ViewControlType;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategory;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryKind;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryService;
import net.ximatai.muyun.spring.platform.metadata.Metadata;
import net.ximatai.muyun.spring.platform.metadata.MetadataAction;
import net.ximatai.muyun.spring.platform.metadata.MetadataActionService;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldDefinitionCompiler;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldConfig;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldConfigService;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldReferenceConfig;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldReferenceConfigService;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.MetadataService;
import net.ximatai.muyun.spring.platform.metadata.MetadataView;
import net.ximatai.muyun.spring.platform.metadata.MetadataViewField;
import net.ximatai.muyun.spring.platform.metadata.MetadataViewFieldService;
import net.ximatai.muyun.spring.platform.metadata.MetadataViewService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldType;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldTypeService;
import net.ximatai.muyun.spring.platform.metadata.RelationRole;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformModuleDefinitionCompilerTest {
    private final TestMemoryDao<PlatformModule> moduleDao = new TestMemoryDao<>();
    private final TestMemoryDao<Metadata> metadataDao = new TestMemoryDao<>();
    private final TestMemoryDao<MetadataField> fieldDao = new TestMemoryDao<>();
    private final TestMemoryDao<PlatformFieldType> fieldTypeDao = new TestMemoryDao<>();
    private final TestMemoryDao<MetadataFieldConfig> fieldConfigDao = new TestMemoryDao<>();
    private final TestMemoryDao<MetadataFieldReferenceConfig> referenceConfigDao = new TestMemoryDao<>();
    private final TestMemoryDao<ModuleMetadataRelation> relationDao = new TestMemoryDao<>();
    private final TestMemoryDao<MetadataView> viewDao = new TestMemoryDao<>();
    private final TestMemoryDao<MetadataViewField> viewFieldDao = new TestMemoryDao<>();
    private final TestMemoryDao<MetadataAction> actionDao = new TestMemoryDao<>();
    private final TestMemoryDao<DictionaryCategory> categoryDao = new TestMemoryDao<>();
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
    private final MetadataActionService actionService = new MetadataActionService(actionDao, relationService);
    private final PlatformModuleDefinitionCompiler compiler =
            new PlatformModuleDefinitionCompiler(moduleService, metadataService, fieldService, fieldDefinitionCompiler,
                    referenceConfigService, relationService, viewService, viewFieldService, actionService);

    {
        fieldTypeService.insert(fieldType("string", FieldType.STRING, 128));
        fieldTypeService.insert(fieldType("id", FieldType.STRING, 32));
        fieldTypeService.insert(fieldType("integer", FieldType.INTEGER, null));
        fieldTypeService.insert(fieldType("boolean", FieldType.BOOLEAN, null));
    }

    @Test
    void shouldCompileMainMetadataIntoDynamicModuleDefinition() {
        moduleService.insert(module("crm.customer", ModuleKind.DYNAMIC));
        String metadataId = metadataService.insert(metadata("crm", "customer"));
        categoryService.insert(category("crm", "customer_status", DictionaryCategoryKind.DICTIONARY));
        fieldService.insert(titleField(metadataId));
        MetadataField status = field(metadataId, "status", "status", FieldType.STRING);
        fieldService.insert(status);
        MetadataFieldConfig statusConfig = fieldConfig(status.getId());
        statusConfig.setDictionaryCategoryAlias("customer_status");
        fieldConfigService.insert(statusConfig);
        fieldService.insert(sortField(metadataId));
        fieldService.insert(enabledField(metadataId));
        fieldService.insert(parentField(metadataId));
        relationService.insert(mainRelation("crm.customer", metadataId));

        ModuleDefinition definition = compiler.compile("crm.customer");

        assertThat(definition.moduleAlias()).isEqualTo("crm.customer");
        assertThat(definition.entities()).hasSize(1);
        assertThat(definition.entities().getFirst().code()).isEqualTo("customer");
        assertThat(definition.entities().getFirst().schemaName()).isEqualTo(MetadataService.DEFAULT_SCHEMA);
        assertThat(definition.entities().getFirst().tableName()).isEqualTo("crm_customer");
        assertThat(definition.entities().getFirst().capabilities())
                .contains(EntityCapability.TREE, EntityCapability.SORT, EntityCapability.REFERENCE, EntityCapability.ENABLE);
        assertThat(definition.entities().getFirst().fields())
                .extracting(FieldDefinition::fieldName)
                .containsExactly("title", "status", "sortOrder", "enabled", "parentId");
        assertThat(definition.entities().getFirst().fields().get(1).dictionaryBinding().categoryAlias())
                .isEqualTo("customer_status");
    }

    @Test
    void shouldCompileChildMetadataRelations() {
        moduleService.insert(module("sales.invoice", ModuleKind.DYNAMIC));
        String invoiceId = metadataService.insert(metadata("sales", "invoice"));
        String lineId = metadataService.insert(metadata("sales", "invoice_line"));
        fieldService.insert(titleField(invoiceId));
        fieldService.insert(titleField(lineId));
        fieldService.insert(field(lineId, "invoiceId", "invoice_id", FieldType.STRING));
        relationService.insert(mainRelation("sales.invoice", invoiceId));
        ModuleMetadataRelation child = childRelation("sales.invoice", lineId, invoiceId);
        child.setAutoPopulate(true);
        child.setCascadeDelete(true);
        relationService.insert(child);

        ModuleDefinition definition = compiler.compile("sales.invoice");

        assertThat(definition.entities()).extracting(entity -> entity.code())
                .containsExactly("invoice", "invoice_line");
        assertThat(definition.entities().getFirst().capabilities()).contains(EntityCapability.CHILD_RELATION);
        assertThat(definition.relations()).hasSize(1);
        EntityRelationDefinition relation = definition.relations().getFirst();
        assertThat(relation.code()).isEqualTo("lines");
        assertThat(relation.parentEntity()).isEqualTo("invoice");
        assertThat(relation.childEntity()).isEqualTo("invoice_line");
        assertThat(relation.childForeignKeyField()).isEqualTo("invoiceId");
        assertThat(relation.autoPopulate()).isTrue();
        assertThat(relation.autoDeleteWithParent()).isTrue();
        assertThat(definition.associationViews()).hasSize(1);
        EntityAssociationViewDefinition associationView = definition.associationViews().getFirst();
        assertThat(associationView.sourceEntity()).isEqualTo("invoice");
        assertThat(associationView.targetModuleAlias()).isEqualTo("sales.invoice");
        assertThat(associationView.targetEntity()).isEqualTo("invoice_line");
        assertThat(associationView.displayMode()).isEqualTo(AssociationViewDisplayMode.INLINE_LIST);
        assertThat(associationView.relationCode()).isEqualTo("lines");
        assertThat(associationView.queryable()).isTrue();
    }

    @Test
    void shouldCompileFieldReferenceConfigIntoModuleDefinition() {
        moduleService.insert(module("sales.invoice", ModuleKind.DYNAMIC));
        String invoiceId = metadataService.insert(metadata("sales", "invoice"));
        String lineId = metadataService.insert(metadata("sales", "invoice_line"));
        fieldService.insert(titleField(invoiceId));
        fieldService.insert(field(invoiceId, "code", "code", FieldType.STRING));
        fieldService.insert(titleField(lineId));
        MetadataField invoiceField = field(lineId, "invoiceId", "invoice_id", FieldType.STRING);
        fieldService.insert(invoiceField);
        relationService.insert(mainRelation("sales.invoice", invoiceId));
        relationService.insert(childRelation("sales.invoice", lineId, invoiceId));
        MetadataFieldReferenceConfig referenceConfig = referenceConfig(invoiceField.getId(), invoiceId);
        referenceConfig.setAutoTitle(true);
        referenceConfig.setTitleOutputField("invoiceTitle");
        referenceConfig.setProjectionMappings("code:invoiceCode");
        referenceConfigService.insert(referenceConfig);

        ModuleDefinition definition = compiler.compile("sales.invoice");

        assertThat(definition.references()).hasSize(1);
        EntityReferenceDefinition reference = definition.references().getFirst();
        assertThat(reference.sourceEntity()).isEqualTo("invoice_line");
        assertThat(reference.sourceField()).isEqualTo("invoiceId");
        assertThat(reference.targetQualifiedName()).isEqualTo("sales.invoice.invoice");
        assertThat(reference.autoTitle()).isTrue();
        assertThat(reference.titleOutputField()).isEqualTo("invoiceTitle");
        assertThat(reference.projections()).hasSize(1);
        assertThat(reference.projections().getFirst().targetField()).isEqualTo("code");
        assertThat(reference.projections().getFirst().outputField()).isEqualTo("invoiceCode");
        assertThat(DynamicModuleDescriptor.from(definition).references().getFirst().targetEntityCode()).isEqualTo("invoice");
        assertThat(definition.associationViews()).extracting(EntityAssociationViewDefinition::code)
                .contains("lines", "invoiceId");
        EntityAssociationViewDefinition referenceView = definition.associationViews().stream()
                .filter(view -> "invoiceId".equals(view.code()))
                .findFirst()
                .orElseThrow();
        assertThat(referenceView.displayMode()).isEqualTo(AssociationViewDisplayMode.LINKED_RECORD);
        assertThat(referenceView.referenceField()).isEqualTo("invoiceId");
        assertThat(referenceView.targetEntity()).isEqualTo("invoice");
        assertThat(referenceView.queryable()).isFalse();
    }

    @Test
    void shouldCompileManyReferenceAssociationViewAsLinkedList() {
        moduleService.insert(module("sales.invoice", ModuleKind.DYNAMIC));
        String invoiceId = metadataService.insert(metadata("sales", "invoice"));
        String lineId = metadataService.insert(metadata("sales", "invoice_line"));
        fieldService.insert(titleField(invoiceId));
        fieldService.insert(titleField(lineId));
        MetadataField invoiceField = field(lineId, "invoiceId", "invoice_id", FieldType.STRING);
        fieldService.insert(invoiceField);
        relationService.insert(mainRelation("sales.invoice", invoiceId));
        relationService.insert(childRelation("sales.invoice", lineId, invoiceId));
        MetadataFieldReferenceConfig referenceConfig = referenceConfig(invoiceField.getId(), invoiceId);
        referenceConfig.setCardinality(ReferenceCardinality.MANY);
        referenceConfigService.insert(referenceConfig);

        ModuleDefinition definition = compiler.compile("sales.invoice");

        EntityAssociationViewDefinition referenceView = definition.associationViews().stream()
                .filter(view -> "invoiceId".equals(view.code()))
                .findFirst()
                .orElseThrow();
        assertThat(referenceView.displayMode()).isEqualTo(AssociationViewDisplayMode.LINKED_LIST);
        assertThat(referenceView.viewType()).isEqualTo(EntityViewType.LIST);
        assertThat(referenceView.queryable()).isTrue();
    }

    @Test
    void shouldCompileRelationScopedFieldConfigForSameMetadataInDifferentModules() {
        moduleService.insert(module("crm.customer", ModuleKind.DYNAMIC));
        moduleService.insert(module("sales.customer", ModuleKind.DYNAMIC));
        String metadataId = metadataService.insert(metadata("crm", "customer"));
        fieldService.insert(titleField(metadataId));
        MetadataField status = field(metadataId, "status", "status", FieldType.STRING);
        fieldService.insert(status);
        String crmRelationId = relationService.insert(mainRelation("crm.customer", metadataId));
        String salesRelationId = relationService.insert(mainRelation("sales.customer", metadataId));
        MetadataFieldConfig crmConfig = fieldConfig(status.getId());
        crmConfig.setRelationId(crmRelationId);
        crmConfig.setQueryable(true);
        fieldConfigService.insert(crmConfig);
        MetadataFieldConfig salesConfig = fieldConfig(status.getId());
        salesConfig.setRelationId(salesRelationId);
        salesConfig.setQueryable(false);
        fieldConfigService.insert(salesConfig);

        ModuleDefinition crmDefinition = compiler.compile("crm.customer");
        ModuleDefinition salesDefinition = compiler.compile("sales.customer");

        assertThat(field(crmDefinition, "status").queryDefinition().queryable()).isTrue();
        assertThat(field(salesDefinition, "status").queryDefinition().queryable()).isFalse();
    }

    @Test
    void shouldCompileRelationScopedReferenceConfigForSameSourceMetadataInDifferentModules() {
        moduleService.insert(module("sales.invoice", ModuleKind.DYNAMIC));
        moduleService.insert(module("support.ticket", ModuleKind.DYNAMIC));
        String invoiceId = metadataService.insert(metadata("sales", "invoice"));
        String ticketId = metadataService.insert(metadata("support", "ticket"));
        String lineId = metadataService.insert(metadata("sales", "work_line"));
        fieldService.insert(titleField(invoiceId));
        fieldService.insert(field(invoiceId, "code", "code", FieldType.STRING));
        fieldService.insert(titleField(ticketId));
        fieldService.insert(field(ticketId, "code", "code", FieldType.STRING));
        fieldService.insert(titleField(lineId));
        MetadataField ownerField = field(lineId, "ownerId", "owner_id", FieldType.STRING);
        fieldService.insert(ownerField);
        relationService.insert(mainRelation("sales.invoice", invoiceId));
        relationService.insert(childRelation("sales.invoice", lineId, invoiceId, "ownerId"));
        relationService.insert(mainRelation("support.ticket", ticketId));
        String supportLineRelationId = relationService.insert(childRelation("support.ticket", lineId, ticketId, "ownerId"));
        MetadataFieldReferenceConfig defaultReference = referenceConfig(ownerField.getId(), invoiceId);
        defaultReference.setAutoTitle(true);
        defaultReference.setTitleOutputField("invoiceTitle");
        defaultReference.setProjectionMappings("code:invoiceCode");
        referenceConfigService.insert(defaultReference);
        MetadataFieldReferenceConfig supportReference = referenceConfig(ownerField.getId(), ticketId);
        supportReference.setRelationId(supportLineRelationId);
        supportReference.setAutoTitle(true);
        supportReference.setTitleOutputField("ticketTitle");
        supportReference.setProjectionMappings("code:ticketCode");
        referenceConfigService.insert(supportReference);

        ModuleDefinition salesDefinition = compiler.compile("sales.invoice");
        ModuleDefinition supportDefinition = compiler.compile("support.ticket");

        EntityReferenceDefinition salesReference = salesDefinition.references().getFirst();
        assertThat(salesReference.targetQualifiedName()).isEqualTo("sales.invoice.invoice");
        assertThat(salesReference.titleOutputField()).isEqualTo("invoiceTitle");
        assertThat(salesReference.projections().getFirst().outputField()).isEqualTo("invoiceCode");
        EntityReferenceDefinition supportReferenceDefinition = supportDefinition.references().getFirst();
        assertThat(supportReferenceDefinition.targetQualifiedName()).isEqualTo("support.ticket.ticket");
        assertThat(supportReferenceDefinition.titleOutputField()).isEqualTo("ticketTitle");
        assertThat(supportReferenceDefinition.projections().getFirst().outputField()).isEqualTo("ticketCode");
    }

    @Test
    void shouldCompileConfiguredViewsIntoModuleDefinition() {
        moduleService.insert(module("crm.customer", ModuleKind.DYNAMIC));
        String metadataId = metadataService.insert(metadata("crm", "customer"));
        MetadataField title = titleField(metadataId);
        fieldService.insert(title);
        MetadataField status = field(metadataId, "status", "status", FieldType.STRING);
        status.setRequired(true);
        fieldService.insert(status);
        String relationId = relationService.insert(mainRelation("crm.customer", metadataId));
        String listViewId = viewService.insert(metadataView(relationId, EntityViewType.LIST, "客户列表"));
        MetadataViewField titleViewField = metadataViewField(listViewId, title.getId());
        titleViewField.setTitle("客户名称");
        viewFieldService.insert(titleViewField);
        MetadataViewField statusViewField = metadataViewField(listViewId, status.getId());
        statusViewField.setControlType(ViewControlType.SELECT);
        statusViewField.setReadOnly(true);
        statusViewField.setRequiredOverride(true);
        viewFieldService.insert(statusViewField);

        ModuleDefinition definition = compiler.compile("crm.customer");

        assertThat(definition.views()).hasSize(1);
        EntityViewDefinition view = definition.views().getFirst();
        assertThat(view.entityCode()).isEqualTo("customer");
        assertThat(view.viewType()).isEqualTo(EntityViewType.LIST);
        assertThat(view.title()).isEqualTo("客户列表");
        assertThat(view.fields()).extracting(field -> field.fieldName())
                .containsExactly("title", "status");
        assertThat(view.fields().getFirst().title()).isEqualTo("客户名称");
        assertThat(view.fields().get(1).controlType()).isEqualTo(ViewControlType.SELECT);
        assertThat(view.fields().get(1).readOnly()).isTrue();
        assertThat(view.fields().get(1).required()).isTrue();
        List<net.ximatai.muyun.spring.dynamic.descriptor.DynamicViewDescriptor> descriptorViews =
                DynamicModuleDescriptor.from(definition).entities().getFirst().views();
        assertThat(descriptorViews).hasSize(2);
        assertThat(descriptorViews.getFirst().fields().get(1).required()).isTrue();
        assertThat(descriptorViews.get(1).viewType()).isEqualTo(EntityViewType.FORM);
    }

    @Test
    void shouldCompileConfiguredActionsIntoModuleDefinition() {
        moduleService.insert(module("crm.customer", ModuleKind.DYNAMIC));
        String metadataId = metadataService.insert(metadata("crm", "customer"));
        fieldService.insert(titleField(metadataId));
        String relationId = relationService.insert(mainRelation("crm.customer", metadataId));
        MetadataAction create = metadataAction(relationId, "create", EntityActionKind.RECORD);
        create.setTitle("新建客户");
        create.setActionLevel(EntityActionLevel.PRIMARY);
        create.setPermissionCode("crm.customer.create");
        actionService.insert(create);
        MetadataAction delete = metadataAction(relationId, "delete", EntityActionKind.RECORD);
        delete.setEnabled(false);
        delete.setTitle("删除客户");
        delete.setActionLevel(EntityActionLevel.DANGER);
        actionService.insert(delete);
        MetadataAction export = metadataAction(relationId, "exportData", EntityActionKind.CUSTOM);
        export.setTitle("导出");
        actionService.insert(export);

        ModuleDefinition definition = compiler.compile("crm.customer");

        assertThat(definition.actions())
                .extracting(action -> action.actionCode())
                .containsExactly("create", "delete", "exportData");
        List<net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor> actions =
                DynamicModuleDescriptor.from(definition).entities().getFirst().actions();
        assertThat(actions.stream().filter(action -> action.code().equals("create")).findFirst())
                .get()
                .satisfies(action -> {
                    assertThat(action.title()).isEqualTo("新建客户");
                    assertThat(action.level()).isEqualTo(EntityActionLevel.PRIMARY);
                    assertThat(action.permissionCode()).isEqualTo("crm.customer.create");
                    assertThat(action.enabled()).isTrue();
                });
        assertThat(actions.stream().filter(action -> action.code().equals("delete")).findFirst())
                .get()
                .satisfies(action -> {
                    assertThat(action.enabled()).isFalse();
                    assertThat(action.level()).isEqualTo(EntityActionLevel.DANGER);
                });
        assertThat(actions.stream().filter(action -> action.code().equals("exportData")).findFirst())
                .get()
                .satisfies(action -> {
                    assertThat(action.kind()).isEqualTo(net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionKind.CUSTOM);
                    assertThat(action.title()).isEqualTo("导出");
                });
    }

    @Test
    void shouldRejectActionConfigThatDoesNotMatchEntityCapability() {
        moduleService.insert(module("crm.customer", ModuleKind.DYNAMIC));
        String metadataId = metadataService.insert(metadata("crm", "customer"));
        fieldService.insert(titleField(metadataId));
        String relationId = relationService.insert(mainRelation("crm.customer", metadataId));
        actionService.insert(metadataAction(relationId, "reorder", EntityActionKind.SORT));

        assertThatThrownBy(() -> compiler.compile("crm.customer"))
                .isInstanceOf(net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException.class)
                .hasMessageContaining("standard action is not supported");
    }

    @Test
    void shouldRejectStaticModuleAndDynamicModuleWithoutMainMetadata() {
        moduleService.insert(module("crm.report", ModuleKind.STATIC));
        moduleService.insert(module("crm.empty", ModuleKind.DYNAMIC));

        assertThatThrownBy(() -> compiler.compile("crm.report"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("DYNAMIC module");
        assertThatThrownBy(() -> compiler.compile("crm.empty"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("MAIN metadata");
    }

    private PlatformModule module(String alias, ModuleKind kind) {
        PlatformModule module = new PlatformModule();
        module.setAlias(alias);
        module.setApplicationAlias(alias.substring(0, alias.indexOf('.')));
        module.setModuleKind(kind);
        module.setTitle(alias);
        return module;
    }

    private Metadata metadata(String applicationAlias, String alias) {
        Metadata metadata = new Metadata();
        metadata.setApplicationAlias(applicationAlias);
        metadata.setAlias(alias);
        metadata.setTitle(alias);
        return metadata;
    }

    private DictionaryCategory category(String applicationAlias, String alias, DictionaryCategoryKind kind) {
        DictionaryCategory category = new DictionaryCategory();
        category.setApplicationAlias(applicationAlias);
        category.setAlias(alias);
        category.setCategoryKind(kind);
        category.setTitle(alias);
        return category;
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

    private MetadataField sortField(String metadataId) {
        MetadataField field = field(metadataId, "sortOrder", "sort_order", FieldType.INTEGER);
        field.setSortableField(true);
        return field;
    }

    private MetadataField enabledField(String metadataId) {
        return field(metadataId, "enabled", "enabled", FieldType.BOOLEAN);
    }

    private MetadataField parentField(String metadataId) {
        MetadataField field = field(metadataId, "parentId", "parent_id", FieldType.STRING);
        field.setFieldTypeAlias("id");
        return field;
    }

    private PlatformFieldType fieldType(String alias, FieldType fieldType, Integer length) {
        PlatformFieldType type = new PlatformFieldType();
        type.setAlias(alias);
        type.setTitle(alias);
        type.setFieldType(fieldType);
        type.setDefaultLength(length);
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

    private MetadataView metadataView(String relationId, EntityViewType viewType, String title) {
        MetadataView view = new MetadataView();
        view.setRelationId(relationId);
        view.setViewType(viewType);
        view.setTitle(title);
        return view;
    }

    private MetadataViewField metadataViewField(String viewId, String fieldId) {
        MetadataViewField viewField = new MetadataViewField();
        viewField.setViewId(viewId);
        viewField.setMetadataFieldId(fieldId);
        return viewField;
    }

    private MetadataAction metadataAction(String relationId, String actionCode, EntityActionKind kind) {
        MetadataAction action = new MetadataAction();
        action.setRelationId(relationId);
        action.setActionCode(actionCode);
        action.setActionKind(kind);
        return action;
    }

    private FieldDefinition field(ModuleDefinition definition, String fieldName) {
        return definition.entities().getFirst().fields().stream()
                .filter(field -> field.fieldName().equals(fieldName))
                .findFirst()
                .orElseThrow();
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
        return childRelation(moduleAlias, metadataId, parentMetadataId, "invoiceId");
    }

    private ModuleMetadataRelation childRelation(String moduleAlias,
                                                 String metadataId,
                                                 String parentMetadataId,
                                                 String foreignKey) {
        ModuleMetadataRelation relation = new ModuleMetadataRelation();
        relation.setModuleAlias(moduleAlias);
        relation.setMetadataId(metadataId);
        relation.setParentMetadataId(parentMetadataId);
        relation.setRelationRole(RelationRole.CHILD);
        relation.setForeignKey(foreignKey);
        relation.setRelationAlias("lines");
        relation.setTitle("lines");
        return relation;
    }

}
