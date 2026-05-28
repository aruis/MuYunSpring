package net.ximatai.muyun.spring.platform.publish;

import net.ximatai.muyun.spring.ability.AbilityException;
import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.platform.metadata.Metadata;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.MetadataService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
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
    private final TestMemoryDao<ModuleMetadataRelation> relationDao = new TestMemoryDao<>();
    private final PlatformModuleService moduleService = new PlatformModuleService(moduleDao);
    private final MetadataService metadataService = new MetadataService(metadataDao);
    private final MetadataFieldService fieldService = new MetadataFieldService(fieldDao, metadataService);
    private final ModuleMetadataRelationService relationService =
            new ModuleMetadataRelationService(relationDao, moduleService, metadataService);
    private final PlatformModuleDefinitionCompiler compiler =
            new PlatformModuleDefinitionCompiler(moduleService, metadataService, fieldService, relationService);

    @Test
    void shouldCompileMainMetadataIntoDynamicModuleDefinition() {
        moduleService.insert(module("crm.customer", ModuleKind.DYNAMIC));
        String metadataId = metadataService.insert(metadata("crm", "customer"));
        fieldService.insert(titleField(metadataId));
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
                .containsExactly("title", "sortOrder", "enabled", "parentId");
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
    }

    @Test
    void shouldRejectStaticModuleAndDynamicModuleWithoutMainMetadata() {
        moduleService.insert(module("crm.report", ModuleKind.STATIC));
        moduleService.insert(module("crm.empty", ModuleKind.DYNAMIC));

        assertThatThrownBy(() -> compiler.compile("crm.report"))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("DYNAMIC module");
        assertThatThrownBy(() -> compiler.compile("crm.empty"))
                .isInstanceOf(AbilityException.class)
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

    private MetadataField field(String metadataId, String fieldName, String columnName, FieldType fieldType) {
        MetadataField field = new MetadataField();
        field.setMetadataId(metadataId);
        field.setFieldName(fieldName);
        field.setColumnName(columnName);
        field.setFieldType(fieldType);
        field.setTitle(fieldName);
        return field;
    }

    private MetadataField titleField(String metadataId) {
        MetadataField field = field(metadataId, "title", "title", FieldType.STRING);
        field.setTitleField(true);
        field.setFieldLength(128);
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
        field.setFieldLength(32);
        return field;
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
        relation.setForeignKey("invoiceId");
        relation.setRelationAlias("lines");
        relation.setTitle("lines");
        return relation;
    }

}
