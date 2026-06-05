package net.ximatai.muyun.spring.platform;

import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.schema.StaticEntityTableMapper;
import net.ximatai.muyun.spring.platform.application.Application;
import net.ximatai.muyun.spring.platform.audit.RuntimeAuditRecord;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategory;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItem;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuScheme;
import net.ximatai.muyun.spring.platform.metadata.Metadata;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldConfig;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldProtectionConfig;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldReferenceConfig;
import net.ximatai.muyun.spring.platform.metadata.MetadataView;
import net.ximatai.muyun.spring.platform.metadata.MetadataViewField;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFormulaRule;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldType;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDefinition;
import net.ximatai.muyun.spring.platform.workflow.WorkflowEvent;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstance;
import net.ximatai.muyun.spring.platform.workflow.WorkflowLinkDefinition;
import net.ximatai.muyun.spring.platform.workflow.WorkflowNodeDefinition;
import net.ximatai.muyun.spring.platform.workflow.WorkflowNodeInstance;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRouteInstance;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTask;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskCheck;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskCheckResult;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskDefinition;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskGuide;
import net.ximatai.muyun.spring.platform.workflow.WorkflowVersion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformModelSchemaTest {
    private final StaticEntityTableMapper mapper = new StaticEntityTableMapper();

    @Test
    void shouldMapApplicationAsSortableEnabledPlatformModel() {
        TableWrapper table = mapper.toTable(Application.class);

        assertThat(table.getName()).isEqualTo("platform_application");
        assertThat(columnNames(table))
                .contains("id", "title", "sort_order", "enabled")
                .doesNotContain("parent_id");
    }

    @Test
    void shouldMapModuleAsTreeEnabledPlatformModel() {
        TableWrapper table = mapper.toTable(PlatformModule.class);

        assertThat(table.getName()).isEqualTo("platform_module");
        assertThat(columnNames(table))
                .contains("id", "application_alias", "title", "parent_id", "sort_order", "enabled",
                        "module_kind", "system_managed");
        assertThat(table.getPrimaryKey().getLength()).isEqualTo(128);
        assertThat(table.getColumns().stream().filter(column -> "parent_id".equals(column.getName())).findFirst())
                .get()
                .satisfies(column -> assertThat(column.getLength()).isEqualTo(128));
        assertThat(table.getColumns().stream().filter(column -> "module_kind".equals(column.getName())).findFirst())
                .get()
                .satisfies(column -> assertThat(column.getLength()).isEqualTo(32));
    }

    @Test
    void shouldMapMetadataModelsAsPlatformTables() {
        assertThat(columnNames(mapper.toTable(Metadata.class)))
                .contains("id", "application_alias", "alias", "schema_name", "table_name", "title", "enabled", "sort_order");
        assertThat(columnNames(mapper.toTable(MetadataField.class)))
                .contains("id", "metadata_id", "field_name", "column_name", "field_type_alias", "required",
                        "unique_field", "indexed", "sortable_field", "title_field")
                .doesNotContain("dictionary_application_alias", "dictionary_category_alias", "queryable");
        assertThat(columnNames(mapper.toTable(PlatformFieldType.class)))
                .contains("id", "alias", "title", "field_type", "default_length", "default_precision",
                        "default_scale", "default_query_operator", "query_operators")
                .doesNotContain("verify_regex");
        assertThat(columnType(mapper.toTable(PlatformFieldType.class), "query_operators"))
                .isEqualTo(ColumnType.JSON_SET);
        assertThat(columnNames(mapper.toTable(MetadataFieldConfig.class)))
                .contains("id", "metadata_field_id", "relation_id", "dictionary_application_alias", "dictionary_category_alias",
                        "selection_mode", "field_length", "precision", "scale", "queryable", "default_query_operator",
                        "query_operators", "default_value", "validation_regex", "copyable", "write_protected")
                .doesNotContain("verify_regex");
        assertThat(columnType(mapper.toTable(MetadataFieldConfig.class), "query_operators"))
                .isEqualTo(ColumnType.JSON_SET);
        assertThat(columnNames(mapper.toTable(MetadataFieldProtectionConfig.class)))
                .contains("id", "metadata_field_id", "enabled", "encryption_mode", "signature_mode", "masking_policy");
        assertThat(columnNames(mapper.toTable(MetadataFieldReferenceConfig.class)))
                .contains("id", "metadata_field_id", "relation_id", "target_module_alias", "target_metadata_id",
                        "cardinality", "auto_title", "title_output_field", "projection_mappings");
        assertThat(columnNames(mapper.toTable(ModuleMetadataRelation.class)))
                .contains("id", "module_alias", "metadata_id", "relation_role", "parent_metadata_id",
                        "foreign_key", "relation_alias", "auto_populate", "cascade_delete", "sort_order");
        assertThat(columnNames(mapper.toTable(MetadataView.class)))
                .contains("id", "relation_id", "view_type", "title", "enabled", "sort_order");
        assertThat(columnNames(mapper.toTable(MetadataViewField.class)))
                .contains("id", "view_id", "metadata_field_id", "visible", "control_type", "read_only",
                        "required_override", "title", "enabled", "sort_order");
        assertThat(columnNames(mapper.toTable(PlatformModuleAction.class)))
                .contains("id", "module_alias", "entity_alias", "action_code", "permission_action_code", "title",
                        "category", "available_expression", "unavailable_message", "executor_type", "executor_key",
                        "action_level", "access_mode", "action_auth", "data_auth", "default_grant_policy",
                        "system_managed", "enabled", "sort_order")
                .doesNotContain("relation_id", "permission_code", "alias");
        assertThat(columnNames(mapper.toTable(ModuleMetadataFormulaRule.class)))
                .contains("id", "relation_id", "alias", "rule_kind", "rule_phase", "target_field",
                        "expression", "severity", "message_template", "stop_on_error", "enabled", "sort_order");
    }

    @Test
    void shouldMapMenuModelsAsPlatformTables() {
        assertThat(columnNames(mapper.toTable(MenuScheme.class)))
                .contains("id", "tenant_id", "alias", "scope_type", "scope_id", "title", "enabled", "sort_order")
                .doesNotContain("parent_id", "application_alias");
        assertThat(columnNames(mapper.toTable(Menu.class)))
                .contains("id", "tenant_id", "scheme_id", "parent_id", "menu_type", "module_alias",
                        "route", "external_url", "title", "enabled", "sort_order")
                .doesNotContain("application_alias");
    }

    @Test
    void shouldMapDictionaryModelsAsPlatformTables() {
        assertThat(columnNames(mapper.toTable(DictionaryCategory.class)))
                .contains("id", "tenant_id", "application_alias", "alias", "category_kind",
                        "parent_id", "title", "enabled", "sort_order");
        assertThat(columnNames(mapper.toTable(DictionaryItem.class)))
                .contains("id", "tenant_id", "application_alias", "category_alias", "code",
                        "parent_id", "title", "enabled", "sort_order");
        assertThat(uniqueIndexes(mapper.toTable(DictionaryCategory.class)))
                .contains(List.of("tenant_id", "application_alias", "alias"));
        assertThat(uniqueIndexes(mapper.toTable(DictionaryItem.class)))
                .contains(List.of("tenant_id", "application_alias", "category_alias", "code"));
    }

    @Test
    void shouldMapRuntimeAuditRecordAsPlatformTable() {
        TableWrapper table = mapper.toTable(RuntimeAuditRecord.class);

        assertThat(table.getName()).isEqualTo("platform_runtime_audit_record");
        assertThat(columnNames(table))
                .contains("id", "tenant_id", "event_id", "trace_id", "event_type", "module_alias",
                        "entity_alias", "record_id", "action_code", "executor_type", "action_level",
                        "result_type", "result_message", "refresh_requested", "redirect_to", "result_text",
                        "failure_stage", "error_message", "error_type", "system_context", "system_reason",
                        "operator_id", "operator_type", "authorization_decision", "authorization_permission_code",
                        "authorization_permission_action_code", "mutation_source",
                        "payload_text", "occurred_at");
        assertThat(uniqueIndexes(table)).contains(List.of("tenant_id", "event_id"));
        assertThat(indexes(table)).contains(
                List.of("trace_id"),
                List.of("tenant_id", "action_code", "occurred_at"),
                List.of("tenant_id", "result_type", "occurred_at")
        );
        assertThat(columnType(table, "payload_text")).isEqualTo(ColumnType.TEXT);
        assertThat(columnType(table, "result_message")).isEqualTo(ColumnType.TEXT);
        assertThat(columnType(table, "redirect_to")).isEqualTo(ColumnType.TEXT);
        assertThat(columnType(table, "result_text")).isEqualTo(ColumnType.TEXT);
        assertThat(columnType(table, "error_message")).isEqualTo(ColumnType.TEXT);
    }

    @Test
    void shouldMapWorkflowModelsAsPlatformTables() {
        assertThat(columnNames(mapper.toTable(WorkflowDefinition.class)))
                .contains("id", "tenant_id", "application_alias", "module_alias", "alias",
                        "title", "approval_enabled", "definition_status", "current_version_no",
                        "enabled", "sort_order")
                .doesNotContain("entity_alias");
        assertThat(uniqueIndexes(mapper.toTable(WorkflowDefinition.class)))
                .contains(List.of("tenant_id", "module_alias", "alias"));
        assertThat(columnNames(mapper.toTable(WorkflowVersion.class)))
                .contains("id", "definition_id", "version_no", "publish_status", "snapshot_text",
                        "published_by", "published_at");
        assertThat(columnNames(mapper.toTable(WorkflowNodeDefinition.class)))
                .contains("id", "workflow_version_id", "node_key", "node_type", "approval_mode",
                        "milestone_type", "converge_mode", "converge_ratio", "task_definition_id",
                        "allow_reject", "require_reject_reason", "allow_reject_return_to_me",
                        "allow_rollback", "require_rollback_reason", "allow_terminate",
                        "require_terminate_reason", "allow_add_sign", "participant_policy_text",
                        "node_config_text", "sort_order");
        assertThat(columnNames(mapper.toTable(WorkflowLinkDefinition.class)))
                .contains("id", "workflow_version_id", "route_key", "source_node_key", "target_node_key",
                        "condition_expression", "default_route", "route_config_text", "sort_order");
        assertThat(columnNames(mapper.toTable(WorkflowTaskDefinition.class)))
                .contains("id", "tenant_id", "module_alias", "alias", "title",
                        "manual_confirm", "task_config_text", "enabled", "sort_order")
                .doesNotContain("entity_alias");
        assertThat(columnNames(mapper.toTable(WorkflowTaskGuide.class)))
                .contains("id", "task_definition_id", "guide_key", "guide_kind", "target_module_alias",
                        "target_action_code", "guide_config_text", "title", "enabled", "sort_order");
        assertThat(columnNames(mapper.toTable(WorkflowTaskCheck.class)))
                .contains("id", "task_definition_id", "check_key", "check_kind", "expression",
                        "failure_message", "check_config_text", "title", "enabled", "sort_order");
        assertThat(columnNames(mapper.toTable(WorkflowInstance.class)))
                .contains("id", "tenant_id", "definition_id", "workflow_version_id", "version_no",
                        "module_alias", "record_id", "approval_enabled",
                        "approval_status", "instance_status", "approval_completed_at", "started_by",
                        "started_at", "completed_at", "terminated_at", "current_node_keys",
                        "reject_resubmit_mode", "reject_return_node_key", "reject_return_owner_id",
                        "previous_instance_id",
                        "last_action_code", "last_action_reason", "last_operator_id", "last_operated_at",
                        "snapshot_text")
                .doesNotContain("entity_alias");
        assertThat(columnNames(mapper.toTable(WorkflowNodeInstance.class)))
                .contains("id", "instance_id", "node_key", "node_run_id", "node_type", "node_status", "approval_mode",
                        "milestone_type", "converge_mode", "converge_ratio", "route_id",
                        "enter_route_id", "branch_run_id", "converge_run_id",
                        "required_route_count", "arrived_route_count", "completed_route_count",
                        "required_task_count", "completed_task_count", "approved_task_count",
                        "rejected_task_count", "rollback_target_node_key",
                        "allow_reject", "require_reject_reason", "allow_reject_return_to_me",
                        "allow_rollback", "require_rollback_reason", "allow_terminate",
                        "require_terminate_reason", "allow_add_sign",
                        "overtime_status", "activated_at", "completed_at",
                        "node_snapshot_text");
        assertThat(columnNames(mapper.toTable(WorkflowRouteInstance.class)))
                .contains("id", "instance_id", "route_key", "route_run_id", "source_node_key", "target_node_key",
                        "branch_node_key", "branch_run_id", "converge_node_key", "converge_run_id",
                        "parent_route_id", "route_depth",
                        "route_status", "route_reason", "condition_matched", "default_route",
                        "selected_by", "selected_at", "arrived_at", "closed_by_route_id", "closed_reason",
                        "invalidated_by_action_id", "invalidated_at");
        assertThat(columnNames(mapper.toTable(WorkflowTask.class)))
                .contains("id", "tenant_id", "instance_id", "node_instance_id", "task_kind",
                        "task_status", "parent_task_id", "origin_task_id", "assignment_kind",
                        "add_sign_mode", "owner_id", "original_assignee_id", "assignee_id", "actual_processor_id",
                        "delegated_from_user_id", "transferred_from_user_id", "decision",
                        "transferred_by", "transferred_at", "added_by", "added_at",
                        "check_status", "check_result_text", "result_message", "assignment_policy_text",
                        "assignment_snapshot_text", "delegation_policy_id", "due_at", "completed_at");
        assertThat(columnNames(mapper.toTable(WorkflowTaskCheckResult.class)))
                .contains("id", "task_id", "check_key", "check_run_id", "check_kind", "check_status", "passed",
                        "checked_at", "failure_message", "result_payload_text");
        assertThat(uniqueIndexes(mapper.toTable(WorkflowTaskCheckResult.class)))
                .contains(List.of("tenant_id", "task_id", "check_key", "check_run_id"));
        assertThat(columnNames(mapper.toTable(WorkflowEvent.class)))
                .contains("id", "tenant_id", "instance_id", "node_instance_id", "task_id", "event_type",
                        "action_code", "operator_id", "message", "payload_text", "occurred_at");
        assertThat(columnType(mapper.toTable(WorkflowInstance.class), "snapshot_text")).isEqualTo(ColumnType.TEXT);
        assertThat(columnType(mapper.toTable(WorkflowNodeDefinition.class), "participant_policy_text"))
                .isEqualTo(ColumnType.TEXT);
        assertThat(columnType(mapper.toTable(WorkflowTask.class), "result_message")).isEqualTo(ColumnType.TEXT);
        assertThat(indexes(mapper.toTable(WorkflowRouteInstance.class)))
                .contains(List.of("instance_id", "route_key"), List.of("instance_id", "route_status"));
    }

    @Test
    void shouldMapStablePlatformDefaults() {
        assertThat(columnDefault(mapper.toTable(PlatformModule.class), "module_kind")).isEqualTo("'static'");
        assertThat(columnDefault(mapper.toTable(PlatformModule.class), "system_managed")).isEqualTo("FALSE");
        assertThat(columnDefault(mapper.toTable(PlatformModuleAction.class), "access_mode")).isEqualTo("'AUTH_REQUIRED'");
        assertThat(columnDefault(mapper.toTable(PlatformModuleAction.class), "action_auth")).isEqualTo("TRUE");
        assertThat(columnDefault(mapper.toTable(PlatformModuleAction.class), "data_auth")).isEqualTo("FALSE");
        assertThat(columnDefault(mapper.toTable(Menu.class), "menu_type")).isEqualTo("'group'");
        assertThat(columnDefault(mapper.toTable(ModuleMetadataRelation.class), "relation_role")).isEqualTo("'main'");
        assertThat(columnDefault(mapper.toTable(MetadataFieldReferenceConfig.class), "cardinality")).isEqualTo("'ONE'");
        assertThat(columnDefault(mapper.toTable(MetadataFieldConfig.class), "queryable")).isNull();
        assertThat(columnDefault(mapper.toTable(MetadataFieldProtectionConfig.class), "enabled")).isEqualTo("TRUE");
        assertThat(columnDefault(mapper.toTable(MetadataField.class), "required")).isEqualTo("FALSE");
        assertThat(columnDefault(mapper.toTable(MetadataViewField.class), "visible")).isEqualTo("TRUE");
        assertThat(columnDefault(mapper.toTable(RuntimeAuditRecord.class), "system_context")).isEqualTo("FALSE");
        assertThat(columnDefault(mapper.toTable(WorkflowDefinition.class), "approval_enabled")).isEqualTo("FALSE");
        assertThat(columnDefault(mapper.toTable(WorkflowInstance.class), "approval_status")).isEqualTo("'none'");
        assertThat(columnDefault(mapper.toTable(WorkflowInstance.class), "instance_status")).isEqualTo("'running'");
        assertThat(columnDefault(mapper.toTable(WorkflowRouteInstance.class), "route_status")).isEqualTo("'candidate'");
        assertThat(columnDefault(mapper.toTable(WorkflowTask.class), "assignment_kind")).isEqualTo("'normal'");
        assertThat(columnDefault(mapper.toTable(WorkflowTask.class), "check_status")).isEqualTo("'not_checked'");
        assertThat(columnDefault(mapper.toTable(WorkflowTaskDefinition.class), "manual_confirm")).isEqualTo("TRUE");
    }

    private Set<String> columnNames(TableWrapper table) {
        Set<String> names = new LinkedHashSet<>();
        if (table.getPrimaryKey() != null) {
            names.add(table.getPrimaryKey().getName());
        }
        table.getColumns().forEach(column -> names.add(column.getName()));
        return names;
    }

    private List<List<String>> uniqueIndexes(TableWrapper table) {
        return table.getIndexes().stream()
                .filter(index -> index.isUnique())
                .map(index -> List.copyOf(index.getColumns()))
                .toList();
    }

    private List<List<String>> indexes(TableWrapper table) {
        return table.getIndexes().stream()
                .map(index -> List.copyOf(index.getColumns()))
                .toList();
    }

    private ColumnType columnType(TableWrapper table, String columnName) {
        return table.getColumns().stream()
                .filter(column -> columnName.equals(column.getName()))
                .findFirst()
                .orElseThrow()
                .getType();
    }

    private String columnDefault(TableWrapper table, String columnName) {
        return table.getColumns().stream()
                .filter(column -> columnName.equals(column.getName()))
                .findFirst()
                .orElseThrow()
                .getDefaultValue();
    }
}
