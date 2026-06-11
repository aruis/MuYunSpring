package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.code.CodeLedgerEntryWebController;
import net.ximatai.muyun.spring.boot.code.CodeIssueLogWebController;
import net.ximatai.muyun.spring.boot.code.CodeRecycleEntryWebController;
import net.ximatai.muyun.spring.boot.code.CodeRuleWebController;
import net.ximatai.muyun.spring.boot.code.CodeSequenceStateWebController;
import net.ximatai.muyun.spring.boot.iam.OrganizationWebController;
import net.ximatai.muyun.spring.boot.iam.RoleWebController;
import net.ximatai.muyun.spring.boot.iam.TenantWebController;
import net.ximatai.muyun.spring.boot.iam.UserAccountWebController;
import net.ximatai.muyun.spring.boot.workflow.WorkflowRuntimeAdminWebController;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.platform.workflow.WorkflowActionPolicyService;
import net.ximatai.muyun.spring.platform.code.CodePreviewService;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleConfigPublishFacade;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleHealthService;
import net.ximatai.muyun.spring.platform.config.LowCodeModulePackageExchangeService;
import net.ximatai.muyun.spring.platform.config.LowCodeModulePackageImportService;
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
            context.registerBean(RoleWebController.class, () -> new RoleWebController(null));
            context.registerBean(UserAccountWebController.class, () -> new UserAccountWebController(null));
            context.refresh();
            StaticModuleDefinitionScanner scanner = new StaticModuleDefinitionScanner(context);

            List<StaticModuleDefinition> definitions = scanner.scan();
            Map<String, StaticModuleDefinition> byAlias = definitions.stream()
                    .collect(Collectors.toMap(StaticModuleDefinition::moduleAlias, Function.identity()));

            assertThat(byAlias.keySet()).containsExactlyInAnyOrder(
                    "iam.tenant", "iam.organization", "iam.role", "iam.user");
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
                assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                        .containsExactly("menu", "create", "view", "update", "delete", "query",
                                "tree", "sort", "enable", "disable");
            });
            assertThat(byAlias.get("iam.role")).satisfies(definition -> {
                assertThat(definition.applicationAlias()).isEqualTo("iam");
                assertThat(definition.title()).isEqualTo("角色管理");
                assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                        .containsExactlyInAnyOrder("menu", "create", "view", "update", "delete", "query",
                                "sort", "enable", "disable", "roleUsers", "rolePermissions");
                assertThat(definition.actions()).filteredOn(action -> action.actionCode().equals("roleUsers"))
                        .singleElement()
                        .satisfies(action -> assertCustomRecordAction(action, "roleUsers", "角色用户"));
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
            context.registerBean(CodeSequenceStateWebController.class);
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
                    .containsExactly("menu", "view", "query");
            assertThat(byAlias.get("platform.code_ledger_entry").actions()).extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactly("menu", "view", "query");
            assertThat(byAlias.get("platform.code_recycle_entry").actions()).extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactly("menu", "view", "query");
            assertThat(byAlias.get("platform.code_issue_log").actions()).extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactly("menu", "view", "query");
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
            context.registerBean(LowCodeModuleConfigPublishFacade.class,
                    () -> mock(LowCodeModuleConfigPublishFacade.class));
            context.registerBean(LowCodeModuleHealthService.class, () -> mock(LowCodeModuleHealthService.class));
            context.registerBean(LowCodeModulePackageExchangeService.class,
                    () -> mock(LowCodeModulePackageExchangeService.class));
            context.registerBean(LowCodeModulePackageImportService.class,
                    () -> mock(LowCodeModulePackageImportService.class));
            context.registerBean(LowCodeGovernanceWebController.class);
            context.refresh();

            StaticModuleDefinition definition = new StaticModuleDefinitionScanner(context).scan().getFirst();

            assertThat(definition.moduleAlias()).isEqualTo("platform.low_code_governance");
            assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactlyInAnyOrder("checkPackageHealth", "publishPackage", "rollbackPackageVersion",
                            "exportCurrentPackage", "exportVersionPackage", "dryRunImportPackage",
                            "prepareImportDraft", "publishImportDraft");
            Map<String, StaticModuleActionDefinition> actions = definition.actions().stream()
                    .collect(Collectors.toMap(StaticModuleActionDefinition::actionCode, Function.identity()));
            assertThat(actions.get("checkPackageHealth").actionLevel()).isEqualTo(EntityActionLevel.LIST);
            assertThat(actions.get("publishPackage").actionLevel()).isEqualTo(EntityActionLevel.LIST);
            assertThat(actions.get("dryRunImportPackage").actionLevel()).isEqualTo(EntityActionLevel.LIST);
            assertThat(actions.get("prepareImportDraft").actionLevel()).isEqualTo(EntityActionLevel.LIST);
            assertThat(actions.get("publishImportDraft").actionLevel()).isEqualTo(EntityActionLevel.LIST);
            assertThat(actions.get("rollbackPackageVersion").actionLevel()).isEqualTo(EntityActionLevel.RECORD);
            assertThat(actions.get("exportCurrentPackage").actionLevel()).isEqualTo(EntityActionLevel.RECORD);
            assertThat(actions.get("exportVersionPackage").actionLevel()).isEqualTo(EntityActionLevel.RECORD);
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
}
