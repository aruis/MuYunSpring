package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.code.CodeLedgerEntryWebController;
import net.ximatai.muyun.spring.boot.code.CodeIssueLogWebController;
import net.ximatai.muyun.spring.boot.code.CodeRecycleEntryWebController;
import net.ximatai.muyun.spring.boot.code.CodeRuleWebController;
import net.ximatai.muyun.spring.boot.code.CodeSequenceStateWebController;
import net.ximatai.muyun.spring.boot.iam.DepartmentWebController;
import net.ximatai.muyun.spring.boot.iam.EmployeeWebController;
import net.ximatai.muyun.spring.boot.iam.OrganizationWebController;
import net.ximatai.muyun.spring.boot.iam.PositionCategoryWebController;
import net.ximatai.muyun.spring.boot.iam.PositionWebController;
import net.ximatai.muyun.spring.boot.iam.RoleWebController;
import net.ximatai.muyun.spring.boot.iam.TenantWebController;
import net.ximatai.muyun.spring.boot.iam.UserAccountWebController;
import net.ximatai.muyun.spring.boot.workflow.WorkflowRuntimeAdminWebController;
import net.ximatai.muyun.spring.boot.workflow.WorkflowDefinitionWebController;
import net.ximatai.muyun.spring.boot.workflow.WorkflowVersionWebController;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.iam.employee.EmployeePositionService;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccountService;
import net.ximatai.muyun.spring.iam.employee.EmployeeDelegationService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowActionPolicyService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDefinitionService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowPublishFacade;
import net.ximatai.muyun.spring.platform.workflow.WorkflowVersionService;
import net.ximatai.muyun.spring.platform.code.CodePreviewService;
import net.ximatai.muyun.spring.platform.code.CodeOpsActionService;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleConfigArchiveFacade;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleHealthService;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;
import net.ximatai.muyun.spring.platform.config.LowCodeModulePackageExchangeService;
import net.ximatai.muyun.spring.platform.config.LowCodeModulePackageImportService;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleTemplateService;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.common.measure.MeasureUnitField;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMeasureUnitConversionMode;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMeasureUnitMode;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class StaticModuleDefinitionScannerTest {
    @Test
    void shouldScanIamStaticModulesAndActionsFromControllerAnnotations() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(TenantWebController.class);
            context.registerBean(OrganizationWebController.class);
            context.registerBean(DepartmentWebController.class);
            context.registerBean(EmployeeWebController.class,
                    () -> new EmployeeWebController(mock(EmployeePositionService.class),
                            mock(EmployeeAccountService.class), mock(EmployeeDelegationService.class)));
            context.registerBean(PositionWebController.class);
            context.registerBean(PositionCategoryWebController.class);
            context.registerBean(RoleWebController.class, () -> new RoleWebController(null));
            context.registerBean(UserAccountWebController.class, () -> new UserAccountWebController(null));
            context.refresh();
            StaticModuleDefinitionScanner scanner = new StaticModuleDefinitionScanner(context);

            List<StaticModuleDefinition> definitions = scanner.scan();
            Map<String, StaticModuleDefinition> byAlias = definitions.stream()
                    .collect(Collectors.toMap(StaticModuleDefinition::moduleAlias, Function.identity()));

            assertThat(byAlias.keySet()).containsExactlyInAnyOrder(
                    "iam.tenant", "iam.organization", "iam.department", "iam.employee",
                    "iam.position_category", "iam.role", "iam.user");
            assertThat(byAlias.get("iam.tenant")).satisfies(definition -> {
                assertThat(definition.applicationAlias()).isEqualTo("iam");
                assertThat(definition.title()).isEqualTo("租户管理");
                assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                        .containsExactly("menu", "create", "view", "update", "delete", "query",
                                "sort", "enable", "disable");
            });
            assertThat(byAlias.get("iam.organization")).satisfies(definition -> {
                assertThat(definition.applicationAlias()).isEqualTo("iam");
                assertThat(definition.title()).isEqualTo("机构管理");
                assertThat(definition.entryType()).isEqualTo(ModuleEntryType.ROUTE);
                assertThat(definition.entryRoute()).isEqualTo("/iam/organizations");
                assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                        .containsExactly("menu", "create", "view", "update", "delete", "query",
                                "tree", "sort", "enable", "disable");
            });
            assertThat(byAlias.get("iam.department")).satisfies(definition -> {
                assertThat(definition.applicationAlias()).isEqualTo("iam");
                assertThat(definition.title()).isEqualTo("部门管理");
                assertThat(definition.entryType()).isEqualTo(ModuleEntryType.ROUTE);
                assertThat(definition.entryRoute()).isEqualTo("/iam/departments");
                assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                        .containsExactlyInAnyOrder("menu", "create", "view", "update", "delete", "query",
                                "tree", "sort", "enable", "disable");
            });
            assertThat(byAlias.get("iam.employee")).satisfies(definition -> {
                assertThat(definition.applicationAlias()).isEqualTo("iam");
                assertThat(definition.title()).isEqualTo("职员管理");
                assertThat(definition.entryType()).isEqualTo(ModuleEntryType.ROUTE);
                assertThat(definition.entryRoute()).isEqualTo("/iam/employees");
                assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                        .containsExactlyInAnyOrder("menu", "create", "view", "update", "delete", "query",
                                "sort", "enable", "disable", "employeePositions", "employeeAccounts",
                                "employeeDelegations", "employeeDelegatedToMe");
                assertThat(definition.actions()).filteredOn(action -> action.actionCode().equals("employeeAccounts"))
                        .singleElement()
                        .satisfies(action -> assertCustomRecordAction(action, "employeeAccounts", "职员账号"));
                assertThat(definition.actions()).filteredOn(action -> action.actionCode().equals("employeePositions"))
                        .singleElement()
                        .satisfies(action -> assertCustomRecordAction(action, "employeePositions", "职员任岗"));
                assertThat(definition.actions()).filteredOn(action -> action.actionCode().equals("employeeDelegations"))
                        .singleElement()
                        .satisfies(action -> assertCustomRecordAction(action, "employeeDelegations", "职员业务代办"));
                assertThat(definition.actions()).filteredOn(action -> action.actionCode().equals("employeeDelegatedToMe"))
                        .singleElement()
                        .satisfies(action -> assertCustomRecordAction(action, "employeeDelegatedToMe", "职员受托代办"));
                assertThat(definition.uiDefinition()).isNotNull();
                assertThat(definition.uiDefinition().views()).hasSize(2);
                assertThat(definition.uiDefinition().views()).filteredOn(view -> view.viewCode().equals("default_list"))
                        .singleElement()
                        .satisfies(view -> {
                            assertThat(view.viewKind()).isEqualTo(ModuleViewKind.LIST);
                            assertThat(view.fields()).extracting(field -> field.fieldRef().fieldName())
                                    .containsExactly("employeeNo", "title", "mobile", "email", "enabled");
                        });
                assertThat(definition.uiDefinition().views()).filteredOn(view -> view.viewCode().equals("default_form"))
                        .singleElement()
                        .satisfies(view -> {
                            assertThat(view.viewKind()).isEqualTo(ModuleViewKind.FORM);
                            assertThat(view.fields()).extracting(field -> field.fieldRef().fieldName())
                                    .containsExactly("organizationId", "departmentId", "employeeNo", "title",
                                            "gender", "mobile", "email", "enabled");
                        });
            });
            assertThat(byAlias.get("iam.position_category")).satisfies(definition -> {
                assertThat(definition.applicationAlias()).isEqualTo("iam");
                assertThat(definition.title()).isEqualTo("岗位管理");
                assertThat(definition.entryType()).isEqualTo(ModuleEntryType.ROUTE);
                assertThat(definition.entryRoute()).isEqualTo("/iam/positions");
                assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                        .containsExactlyInAnyOrder("menu", "create", "view", "update", "delete", "query",
                                "tree", "sort", "enable", "disable",
                                "position_create", "position_view", "position_update", "position_delete",
                                "position_query", "position_sort", "position_enable", "position_disable");
                assertThat(definition.actions())
                        .filteredOn(action -> action.actionCode().equals("position_query"))
                        .singleElement()
                        .satisfies(action -> {
                            assertThat(action.permissionActionCode()).isEqualTo("position_view");
                            assertThat(action.title()).isEqualTo("查询岗位");
                        });
            });
            assertThat(byAlias.get("iam.role")).satisfies(definition -> {
                assertThat(definition.applicationAlias()).isEqualTo("iam");
                assertThat(definition.title()).isEqualTo("角色管理");
                assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                        .containsExactlyInAnyOrder("menu", "create", "view", "update", "delete", "query",
                                "sort", "enable", "disable", "roleGrants", "rolePermissions");
                assertThat(definition.actions()).filteredOn(action -> action.actionCode().equals("roleGrants"))
                        .singleElement()
                        .satisfies(action -> assertCustomRecordAction(action, "roleGrants", "角色授权实例"));
                assertThat(definition.actions()).filteredOn(action -> action.actionCode().equals("rolePermissions"))
                        .singleElement()
                        .satisfies(action -> assertCustomRecordAction(action, "rolePermissions", "角色授权"));
            });
            assertThat(byAlias.get("iam.user")).satisfies(definition -> {
                assertThat(definition.applicationAlias()).isEqualTo("iam");
                assertThat(definition.moduleAlias()).isEqualTo("iam.user");
                assertThat(definition.title()).isEqualTo("用户管理");
                assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                        .containsExactlyInAnyOrder("menu", "create", "view", "update", "delete", "query",
                                "sort", "enable", "disable", "userSelector", "changePassword");
                assertThat(definition.actions()).filteredOn(action -> action.actionCode().equals("userSelector"))
                        .singleElement()
                        .satisfies(action -> {
                            assertThat(action.title()).isEqualTo("用户选择器");
                            assertThat(action.actionLevel()).isEqualTo(EntityActionLevel.LIST);
                            assertThat(action.dataAuth()).isTrue();
                        });
                assertThat(definition.actions()).filteredOn(action -> action.actionCode().equals("changePassword"))
                        .singleElement()
                        .satisfies(action -> {
                            assertThat(action.title()).isEqualTo("修改密码");
                            assertThat(action.dataAuth()).isTrue();
                        });
            });
        }
    }

    @Test
    void shouldScanCodeRuleAndReadOnlyLifecycleModules() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(CodeRuleWebController.class,
                    () -> new CodeRuleWebController(org.mockito.Mockito.mock(CodePreviewService.class)));
            context.registerBean(CodeSequenceStateWebController.class,
                    () -> new CodeSequenceStateWebController(org.mockito.Mockito.mock(CodeOpsActionService.class)));
            context.registerBean(CodeLedgerEntryWebController.class);
            context.registerBean(CodeRecycleEntryWebController.class);
            context.registerBean(CodeIssueLogWebController.class);
            context.refresh();
            StaticModuleDefinitionScanner scanner = new StaticModuleDefinitionScanner(context);

            Map<String, StaticModuleDefinition> byAlias = scanner.scan().stream()
                    .collect(Collectors.toMap(StaticModuleDefinition::moduleAlias, Function.identity()));

            assertThat(byAlias.keySet()).containsExactlyInAnyOrder(
                    "platform.code_rule",
                    "platform.code_sequence_state",
                    "platform.code_ledger_entry",
                    "platform.code_recycle_entry",
                    "platform.code_issue_log");
            assertThat(byAlias.get("platform.code_rule").actions()).extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactlyInAnyOrder("menu", "view", "query",
                            "sort", "enable", "disable", "viewTree", "saveTree", "preview", "opsQuery", "opsManage");
            assertThat(byAlias.get("platform.code_sequence_state").actions()).extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactlyInAnyOrder("menu", "view", "query", "adjustBaseline");
            assertThat(byAlias.get("platform.code_sequence_state").actions())
                    .filteredOn(action -> action.actionCode().equals("adjustBaseline"))
                    .singleElement()
                    .satisfies(action -> {
                        assertThat(action.actionLevel()).isEqualTo(EntityActionLevel.RECORD);
                        assertThat(action.actionAuth()).isTrue();
                        assertThat(action.dataAuth()).isTrue();
                        assertThat(action.defaultGrantPolicy()).isEqualTo(ActionDefaultGrantPolicy.NONE);
                    });
            assertThat(byAlias.get("platform.code_ledger_entry").actions()).extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactly("menu", "view", "query");
            assertThat(byAlias.get("platform.code_recycle_entry").actions()).extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactly("menu", "view", "query");
            assertThat(byAlias.get("platform.code_issue_log").actions()).extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactly("menu", "view", "query");
        }
    }

    @Test
    void shouldScanMenuMaintenanceModules() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(MenuSchemeWebController.class);
            context.registerBean(MenuManagementWebController.class);
            context.registerBean(DictionaryCategoryWebController.class);
            context.registerBean(DictionaryItemWebController.class);
            context.refresh();
            StaticModuleDefinitionScanner scanner = new StaticModuleDefinitionScanner(context);

            Map<String, StaticModuleDefinition> byAlias = scanner.scan().stream()
                    .collect(Collectors.toMap(StaticModuleDefinition::moduleAlias, Function.identity()));

            assertThat(byAlias.keySet()).containsExactlyInAnyOrder(
                    "platform.menu_scheme", "platform.menu", "platform.dictionary_category");
            assertThat(byAlias.get("platform.menu_scheme").actions())
                    .extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactlyInAnyOrder("menu", "create", "view", "update", "delete", "query",
                            "sort", "enable", "disable");
            assertThat(byAlias.get("platform.menu").actions())
                    .extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactlyInAnyOrder("create", "view", "update", "delete", "query",
                            "tree", "sort", "enable", "disable");
            assertThat(byAlias.get("platform.dictionary_category").actions())
                    .extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactlyInAnyOrder("menu", "create", "view", "update", "delete", "query",
                            "tree", "sort", "enable", "disable",
                            "item_create", "item_view", "item_update", "item_delete", "item_query",
                            "item_tree", "item_sort", "item_enable", "item_disable");
            assertThat(byAlias.get("platform.dictionary_category").actions())
                    .filteredOn(action -> action.actionCode().equals("item_query"))
                    .singleElement()
                    .satisfies(action -> {
                        assertThat(action.permissionActionCode()).isEqualTo("item_view");
                        assertThat(action.title()).isEqualTo("查询字典项");
                    });
        }
    }

    @Test
    void shouldRejectActionContributionConflictingWithTargetModuleAction() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(ConflictingDictionaryCategoryWeb.class);
            context.registerBean(DictionaryItemWebController.class);
            context.refresh();
            StaticModuleDefinitionScanner scanner = new StaticModuleDefinitionScanner(context);

            assertThatThrownBy(scanner::scan)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("action conflicts with target module")
                    .hasMessageContaining("platform.dictionary_category.item_query");
        }
    }

    @Test
    void shouldScanRecordLinkageRuleConfigurationModules() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(RecordGenerationRuleWebController.class);
            context.registerBean(RecordWriteBackRuleWebController.class);
            context.refresh();
            StaticModuleDefinitionScanner scanner = new StaticModuleDefinitionScanner(context);

            Map<String, StaticModuleDefinition> byAlias = scanner.scan().stream()
                    .collect(Collectors.toMap(StaticModuleDefinition::moduleAlias, Function.identity()));

            assertThat(byAlias.keySet()).containsExactlyInAnyOrder(
                    "platform.record_generation_rule", "platform.record_write_back_rule");
            assertThat(byAlias.get("platform.record_generation_rule").actions())
                    .extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactlyInAnyOrder("query", "delete", "enable", "disable",
                            "sort", "viewTree", "saveTree");
            assertThat(byAlias.get("platform.record_write_back_rule").actions())
                    .extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactlyInAnyOrder("query", "delete", "enable", "disable",
                            "sort", "viewTree", "saveTree");
        }
    }

    @Test
    void shouldAssembleWorkflowActionsFromStaticModuleCapabilities() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(WorkflowEnabledWeb.class);
            context.refresh();
            StaticModuleDefinition definition = new StaticModuleDefinitionScanner(context).scan().getFirst();

            assertThat(definition.moduleAlias()).isEqualTo("sales.contract");
            assertThat(definition.supports(EntityCapability.WORKFLOW)).isTrue();
            assertThat(definition.supports(EntityCapability.APPROVAL)).isTrue();
            assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactly("submitApproval");
            assertThat(definition.actions()).filteredOn(action -> action.actionCode().equals("submitApproval"))
                    .singleElement()
                    .satisfies(action -> {
                        assertThat(action.category()).isEqualTo(EntityActionCategory.WORKFLOW);
                        assertThat(action.actionLevel()).isEqualTo(EntityActionLevel.RECORD);
                        assertThat(action.executorType()).isEqualTo(EntityActionExecutorType.SERVICE);
                        assertThat(action.executorKey()).isEqualTo("platform.workflow");
                        assertThat(action.dataAuth()).isFalse();
                    });
        }
    }

    @Test
    void shouldScanWorkflowAdminManagementActions() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(WorkflowRuntimeAdminWebController.class,
                    () -> new WorkflowRuntimeAdminWebController(null));
            context.refresh();
            StaticModuleDefinition definition = new StaticModuleDefinitionScanner(context).scan().getFirst();

            assertThat(definition.applicationAlias()).isEqualTo("platform");
            assertThat(definition.moduleAlias()).isEqualTo(WorkflowActionPolicyService.MANAGEMENT_MODULE_ALIAS);
            assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactlyInAnyOrder(
                            "menu",
                            WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION,
                            WorkflowActionPolicyService.MANAGEMENT_TODO_TASK_QUERY_ACTION,
                            WorkflowActionPolicyService.MANAGEMENT_FORCE_APPROVE_ACTION,
                            WorkflowActionPolicyService.MANAGEMENT_FORCE_TERMINATE_ACTION,
                            WorkflowActionPolicyService.MANAGEMENT_RESET_ACTION,
                            WorkflowActionPolicyService.MANAGEMENT_DELETE_HISTORY_ACTION);
            assertThat(definition.actions()).allSatisfy(action -> {
                assertThat(action.actionLevel()).isEqualTo(EntityActionLevel.LIST);
                assertThat(action.actionAuth()).isTrue();
                assertThat(action.dataAuth()).isFalse();
            });
        }
    }

    @Test
    void shouldScanWorkflowConfigurationModules() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(WorkflowDefinitionWebController.class,
                    () -> new WorkflowDefinitionWebController(mock(net.ximatai.muyun.spring.platform.module.PlatformModuleService.class),
                            mock(WorkflowPublishFacade.class)));
            context.registerBean(WorkflowVersionWebController.class,
                    () -> new WorkflowVersionWebController(mock(WorkflowDefinitionService.class)));
            context.refresh();
            StaticModuleDefinitionScanner scanner = new StaticModuleDefinitionScanner(context);

            Map<String, StaticModuleDefinition> byAlias = scanner.scan().stream()
                    .collect(Collectors.toMap(StaticModuleDefinition::moduleAlias, Function.identity()));

            assertThat(byAlias.keySet()).containsExactlyInAnyOrder(
                    WorkflowDefinitionService.MODULE_ALIAS, WorkflowVersionService.MODULE_ALIAS);
            assertThat(byAlias.get(WorkflowDefinitionService.MODULE_ALIAS).actions())
                    .extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactlyInAnyOrder("create", "view", "update", "delete", "query", "sort",
                            "publishWorkflowDefinition", "disableWorkflowDefinition", "archiveWorkflowDefinition");
            assertThat(byAlias.get(WorkflowVersionService.MODULE_ALIAS).actions())
                    .extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactlyInAnyOrder("create", "view", "update", "delete", "query");
        }
    }

    @Test
    void shouldUseLastModuleSegmentAsStaticEntityAlias() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(MultiSegmentModuleWeb.class, () -> new MultiSegmentModuleWeb(new MultiSegmentModuleService()));
            context.refresh();

            StaticModuleDefinition definition = new StaticModuleDefinitionScanner(context).scan().getFirst();

            assertThat(definition.moduleAlias()).isEqualTo("platform.workflow.definition");
            assertThat(definition.entities()).singleElement()
                    .satisfies(entity -> assertThat(entity.alias()).isEqualTo("definition"));
        }
    }

    @Test
    void shouldScanSnakeCaseWebScopeForCamelCaseStaticAlias() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(PlatformFieldTypeWebController.class);
            context.refresh();

            StaticModuleDefinition definition = new StaticModuleDefinitionScanner(context).scan().getFirst();

            assertThat(definition.moduleAlias()).isEqualTo("platform.field_type");
            assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactly("menu", "create", "view", "update", "delete", "query",
                            "sort", "enable", "disable");
        }
    }

    @Test
    void shouldScanNestedResourceControllerActionsFromInheritedEndpoints() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(PlatformUiSetWebController.class);
            context.refresh();

            StaticModuleDefinition definition = new StaticModuleDefinitionScanner(context).scan().getFirst();

            assertThat(definition.moduleAlias()).isEqualTo("platform.ui_set");
            assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactlyInAnyOrder("query", "view", "create", "update", "delete",
                            "enable", "disable", "sort");
        }
    }

    @Test
    void shouldScanFieldUiTypeNestedConfigurationActions() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(PlatformFieldUiTypeAttributeWebController.class);
            context.registerBean(PlatformFieldUiTypeFieldMappingWebController.class);
            context.refresh();

            Map<String, StaticModuleDefinition> byAlias = new StaticModuleDefinitionScanner(context).scan().stream()
                    .collect(Collectors.toMap(StaticModuleDefinition::moduleAlias, Function.identity()));

            assertThat(byAlias.keySet()).containsExactlyInAnyOrder(
                    "platform.field_ui_type_attribute",
                    "platform.field_ui_type_field_mapping");
            assertThat(byAlias.get("platform.field_ui_type_attribute").actions())
                    .extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactlyInAnyOrder("query", "view", "create", "update", "delete", "sort");
            assertThat(byAlias.get("platform.field_ui_type_field_mapping").actions())
                    .extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactlyInAnyOrder("query", "view", "create", "update", "delete", "sort");
        }
    }

    @Test
    void shouldRejectStaticModuleScopeWhenSeparatorsAreMissing() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(MissingSeparatorAliasWeb.class);
            context.refresh();
            StaticModuleDefinitionScanner scanner = new StaticModuleDefinitionScanner(context);

            assertThatThrownBy(scanner::scan)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("alias must match web scope");
        }
    }

    @Test
    void shouldRegisterPageConfigPublishActionsAsRecordActions() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(PlatformPageConfigPublishWebController.class);
            context.refresh();

            StaticModuleDefinition definition = new StaticModuleDefinitionScanner(context).scan().getFirst();

            assertThat(definition.moduleAlias()).isEqualTo("platform.page_config_publish");
            assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactlyInAnyOrder("publishUiConfig", "unpublishUiConfig",
                            "publishQueryTemplate", "unpublishQueryTemplate");
            assertThat(definition.actions()).allSatisfy(action ->
                    assertThat(action.actionLevel()).isEqualTo(EntityActionLevel.RECORD));
        }
    }

    @Test
    void shouldRegisterLowCodeGovernanceActionsAsStaticModuleActions() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(LowCodeModuleConfigArchiveFacade.class,
                    () -> mock(LowCodeModuleConfigArchiveFacade.class));
            context.registerBean(LowCodeModuleHealthService.class, () -> mock(LowCodeModuleHealthService.class));
            context.registerBean(LowCodeModulePackageExchangeService.class,
                    () -> mock(LowCodeModulePackageExchangeService.class));
            context.registerBean(LowCodeModulePackageImportService.class,
                    () -> mock(LowCodeModulePackageImportService.class));
            context.registerBean(LowCodeModuleTemplateService.class,
                    () -> mock(LowCodeModuleTemplateService.class));
            context.registerBean(LowCodeGovernanceWebController.class);
            context.refresh();

            StaticModuleDefinition definition = new StaticModuleDefinitionScanner(context).scan().getFirst();

            assertThat(definition.moduleAlias()).isEqualTo("platform.low_code_governance");
            assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactlyInAnyOrder("menu",
                            "checkPackageHealth", "archivePackage", "switchCurrentPackageVersion",
                            "exportCurrentPackage", "exportVersionPackage", "dryRunImportPackage",
                            "prepareImportDraft", "archiveImportDraft",
                            "createTemplateFromVersion", "instantiateTemplate");
            Map<String, StaticModuleActionDefinition> actions = definition.actions().stream()
                    .collect(Collectors.toMap(StaticModuleActionDefinition::actionCode, Function.identity()));
            assertThat(actions.get("checkPackageHealth").actionLevel()).isEqualTo(EntityActionLevel.LIST);
            assertThat(actions.get("archivePackage").actionLevel()).isEqualTo(EntityActionLevel.LIST);
            assertThat(actions.get("dryRunImportPackage").actionLevel()).isEqualTo(EntityActionLevel.LIST);
            assertThat(actions.get("prepareImportDraft").actionLevel()).isEqualTo(EntityActionLevel.LIST);
            assertThat(actions.get("archiveImportDraft").actionLevel()).isEqualTo(EntityActionLevel.LIST);
            assertThat(actions.get("createTemplateFromVersion").actionLevel()).isEqualTo(EntityActionLevel.LIST);
            assertThat(actions.get("instantiateTemplate").actionLevel()).isEqualTo(EntityActionLevel.LIST);
            assertThat(actions.get("switchCurrentPackageVersion").actionLevel()).isEqualTo(EntityActionLevel.RECORD);
            assertThat(actions.get("exportCurrentPackage").actionLevel()).isEqualTo(EntityActionLevel.RECORD);
            assertThat(actions.get("exportVersionPackage").actionLevel()).isEqualTo(EntityActionLevel.RECORD);
        }
    }

    @Test
    void shouldCompileStaticServiceModelMeasureUnitFieldsIntoModuleDefinition() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            StaticMeasureOrderService service = new StaticMeasureOrderService();
            context.registerBean(StaticMeasureOrderWeb.class, () -> new StaticMeasureOrderWeb(service));
            context.refresh();

            StaticModuleDefinition definition = new StaticModuleDefinitionScanner(context).scan().getFirst();

            assertThat(definition.moduleAlias()).isEqualTo("sales.order_line");
            assertThat(definition.entities()).singleElement().satisfies(entity -> {
                assertThat(entity.alias()).isEqualTo("order_line");
                assertThat(entity.tableName()).isEqualTo("sales_order_line");
                assertThat(entity.fields()).extracting("fieldName")
                        .contains("quantity", "quantityUnit", "quantityBase", "skuId");
                assertThat(entity.fields()).filteredOn(field -> field.fieldName().equals("quantity"))
                        .singleElement()
                        .satisfies(field -> assertThat(field.measureUnit()).satisfies(measureUnit -> {
                            assertThat(measureUnit.categoryAlias()).isEqualTo("quantity");
                            assertThat(measureUnit.mode()).isEqualTo(FieldMeasureUnitMode.SELECTABLE);
                            assertThat(measureUnit.unitFieldName()).isEqualTo("quantityUnit");
                            assertThat(measureUnit.baseValueFieldName()).isEqualTo("quantityBase");
                            assertThat(measureUnit.baseUnitCode()).isEqualTo("bottle");
                            assertThat(measureUnit.conversionMode()).isEqualTo(FieldMeasureUnitConversionMode.BUSINESS_RULE);
                            assertThat(measureUnit.conversionScopeFieldName()).isEqualTo("skuId");
                        }));
            });
        }
    }

    private void assertCustomRecordAction(StaticModuleActionDefinition action, String actionCode, String title) {
        assertThat(action.actionCode()).isEqualTo(actionCode);
        assertThat(action.permissionActionCode()).isEqualTo(actionCode);
        assertThat(action.title()).isEqualTo(title);
        assertThat(action.actionLevel()).isEqualTo(EntityActionLevel.RECORD);
        assertThat(action.accessMode()).isEqualTo(EntityActionAccessMode.AUTH_REQUIRED);
        assertThat(action.actionAuth()).isTrue();
        assertThat(action.dataAuth()).isTrue();
        assertThat(action.defaultGrantPolicy()).isEqualTo(ActionDefaultGrantPolicy.NONE);
    }

    @Test
    void shouldRejectStaticModuleAliasDifferentFromWebScope() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(BadAliasWeb.class);
            context.refresh();
            StaticModuleDefinitionScanner scanner = new StaticModuleDefinitionScanner(context);

            assertThatThrownBy(scanner::scan)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("alias must match web scope");
        }
    }

    @RestController
    @PlatformStaticModule(application = "iam", alias = "iam.bad", title = "Bad")
    @RequestMapping("/iam.good")
    static class BadAliasWeb extends net.ximatai.muyun.spring.boot.web.WebSupport<Object> {
    }

    @RestController
    @PlatformStaticModule(application = "platform", alias = "platform.field_type", title = "Bad")
    @RequestMapping("/platform.fieldtype")
    static class MissingSeparatorAliasWeb extends net.ximatai.muyun.spring.boot.web.WebSupport<Object> {
    }

    @RestController
    @PlatformStaticModule(application = "sales", alias = "sales.contract", title = "合同",
            capabilities = EntityCapability.APPROVAL)
    static class WorkflowEnabledWeb {
    }

    @RestController
    @PlatformStaticModule(application = "platform", alias = "platform.dictionary_category", title = "字典管理")
    static class ConflictingDictionaryCategoryWeb {
        @CustomActionEndpoint("item_query")
        public void itemQuery() {
        }
    }

    @RestController
    @PlatformStaticModule(application = "sales", alias = "sales.order_line", title = "订单明细")
    static class StaticMeasureOrderWeb extends net.ximatai.muyun.spring.boot.web.WebSupport<StaticMeasureOrderService> {
        StaticMeasureOrderWeb(StaticMeasureOrderService service) {
            this.service = service;
        }
    }

    private static class StaticMeasureOrderService extends AbstractAbilityService<StaticMeasureOrderLine> {
        @SuppressWarnings("unchecked")
        StaticMeasureOrderService() {
            super("sales.order_line", StaticMeasureOrderLine.class, mock(BaseDao.class));
        }
    }

    @RestController
    @PlatformStaticModule(application = "platform", alias = "platform.workflow.definition", title = "流程定义")
    @RequestMapping("/platform.workflow.definition")
    static class MultiSegmentModuleWeb extends net.ximatai.muyun.spring.boot.web.WebSupport<MultiSegmentModuleService> {
        MultiSegmentModuleWeb(MultiSegmentModuleService service) {
            this.service = service;
        }
    }

    private static class MultiSegmentModuleService extends AbstractAbilityService<StaticMeasureOrderLine> {
        @SuppressWarnings("unchecked")
        MultiSegmentModuleService() {
            super("platform.workflow.definition", StaticMeasureOrderLine.class, mock(BaseDao.class));
        }
    }

    @Table(name = "sales_order_line", comment = "Sales order line")
    private static class StaticMeasureOrderLine extends StandardEntity {
        @MeasureUnitField(
                categoryAlias = "quantity",
                defaultUnitCode = "box",
                unitFieldName = "quantityUnit",
                baseValueFieldName = "quantityBase",
                baseUnitCode = "bottle",
                conversionMode = MeasureUnitField.ConversionMode.BUSINESS_RULE,
                conversionScopeFieldName = "skuId"
        )
        @Column(name = "quantity", type = ColumnType.NUMERIC, precision = 18, scale = 2)
        private java.math.BigDecimal quantity;

        @Column(name = "quantity_unit", type = ColumnType.VARCHAR, length = 64)
        private String quantityUnit;

        @Column(name = "quantity_base", type = ColumnType.NUMERIC, precision = 18, scale = 2)
        private java.math.BigDecimal quantityBase;

        @Column(name = "sku_id", type = ColumnType.VARCHAR, length = 64)
        private String skuId;
    }
}
