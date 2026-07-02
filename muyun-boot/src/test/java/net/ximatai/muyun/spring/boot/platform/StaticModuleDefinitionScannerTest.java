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
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
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
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryService;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItemService;
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
            List<StaticModuleDefinition> definitions = scanner(
                    new TenantWebController(),
                    new OrganizationWebController(),
                    new DepartmentWebController(),
                    new EmployeeWebController(mock(EmployeePositionService.class),
                            mock(EmployeeAccountService.class), mock(EmployeeDelegationService.class)),
                    new PositionWebController(),
                    new PositionCategoryWebController(),
                    new RoleWebController(null),
                    new UserAccountWebController(null)
            ).scan();
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
                            assertThat(view.fields()).filteredOn(field -> field.fieldRef().fieldName().equals("departmentId"))
                                    .singleElement()
                                    .satisfies(field -> assertThat(field.uiType()).isEqualTo("recordPicker"));
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
                assertThat(definition.uiDefinition()).isNotNull();
                assertThat(definition.uiDefinition().views())
                        .filteredOn(view -> view.viewCode()
                                .equals(ModuleUiViewCodes.childResourceDefaultForm("position")))
                        .singleElement()
                        .satisfies(view -> {
                            assertThat(view.viewKind()).isEqualTo(ModuleViewKind.FORM);
                            assertThat(view.fields()).extracting(field -> field.fieldRef().relationCode())
                                    .containsExactly("position", "position", "position", "position", "position");
                            assertThat(view.fields()).extracting(field -> field.fieldRef().fieldName())
                                    .containsExactly("categoryId", "code", "title", "description", "enabled");
                            assertThat(view.fields()).filteredOn(field -> field.fieldRef().fieldName().equals("categoryId"))
                                    .singleElement()
                                    .satisfies(field -> {
                                        assertThat(field.label()).isEqualTo("所属分类");
                                        assertThat(field.required().constant()).isTrue();
                                    });
                            assertThat(view.fields()).filteredOn(field -> field.fieldRef().fieldName().equals("enabled"))
                                    .singleElement()
                                    .satisfies(field -> assertThat(field.uiType()).isEqualTo("enabledStatus"));
                        });
            });
            assertThat(byAlias.get("iam.role")).satisfies(definition -> {
                assertThat(definition.applicationAlias()).isEqualTo("iam");
                assertThat(definition.title()).isEqualTo("角色管理");
                assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                        .containsExactlyInAnyOrder("menu", "create", "view", "update", "delete", "query",
                                "sort", "enable", "disable", "accountRoleGrants", "employmentRoleGrants",
                                "rolePermissions");
                assertThat(definition.actions()).filteredOn(action -> action.actionCode().equals("accountRoleGrants"))
                        .singleElement()
                        .satisfies(action -> assertCustomRecordAction(action, "accountRoleGrants", "账号角色授权"));
                assertThat(definition.actions()).filteredOn(action -> action.actionCode().equals("employmentRoleGrants"))
                        .singleElement()
                        .satisfies(action -> assertCustomRecordAction(action, "employmentRoleGrants", "任职角色授权"));
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

    @Test
    void shouldScanCodeRuleAndReadOnlyLifecycleModules() {
            Map<String, StaticModuleDefinition> byAlias = scanner(
                    new CodeRuleWebController(mock(CodePreviewService.class)),
                    new CodeSequenceStateWebController(mock(CodeOpsActionService.class)),
                    new CodeLedgerEntryWebController(),
                    new CodeRecycleEntryWebController(),
                    new CodeIssueLogWebController()
            ).scan().stream()
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

    @Test
    void shouldScanMenuMaintenanceModules() {
            DictionaryCategoryService categoryService = new DictionaryCategoryService(mock(BaseDao.class));
            DictionaryItemService itemService = new DictionaryItemService(mock(BaseDao.class), categoryService);
            Map<String, StaticModuleDefinition> byAlias = scanner(
                    new MenuSchemeWebController(),
                    new MenuManagementWebController(),
                    new TestDictionaryCategoryWebController(categoryService),
                    new TestDictionaryItemWebController(itemService)
            ).scan().stream()
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
            assertThat(byAlias.get("platform.dictionary_category").entities())
                    .extracting(EntityDefinition::alias)
                    .containsExactly("dictionary_category", "item");
            assertThat(byAlias.get("platform.dictionary_category").uiDefinition()).isNotNull();
            assertThat(byAlias.get("platform.dictionary_category").uiDefinition().views())
                    .extracting(ViewDefinition::viewCode)
                    .containsExactly(ModuleUiViewCodes.DEFAULT_FORM,
                            ModuleUiViewCodes.childResourceDefaultForm("item"));
            assertThat(byAlias.get("platform.dictionary_category").uiDefinition().views())
                    .filteredOn(view -> view.viewCode().equals("default_form"))
                    .singleElement()
                    .satisfies(view -> {
                        assertThat(view.viewKind()).isEqualTo(ModuleViewKind.FORM);
                        assertThat(view.fields()).extracting(field -> field.fieldRef().fieldName())
                                .containsExactly("applicationAlias", "alias", "categoryKind", "title", "enabled");
                        assertThat(view.fields()).filteredOn(field -> field.fieldRef().fieldName().equals("categoryKind"))
                                .singleElement()
                                .satisfies(field -> assertThat(field.uiType()).isEqualTo("select"));
                    });
            assertThat(byAlias.get("platform.dictionary_category").uiDefinition().views())
                    .filteredOn(view -> view.viewCode().equals(ModuleUiViewCodes.childResourceDefaultForm("item")))
                    .singleElement()
                    .satisfies(view -> {
                        assertThat(view.viewKind()).isEqualTo(ModuleViewKind.FORM);
                        assertThat(view.fields()).extracting(field -> field.fieldRef().fieldName())
                                .containsExactly("categoryId", "code", "title", "parentId", "enabled");
                        assertThat(view.fields()).extracting(field -> field.fieldRef().relationCode())
                                .containsOnly("item");
                        assertThat(view.fields()).filteredOn(field -> field.fieldRef().fieldName().equals("parentId"))
                                .singleElement()
                                .satisfies(field -> assertThat(field.uiType()).isEqualTo("recordPicker"));
                        assertThat(view.fields()).filteredOn(field -> field.fieldRef().fieldName().equals("enabled"))
                                .singleElement()
                                .satisfies(field -> assertThat(field.uiType()).isEqualTo("enabledStatus"));
                    });
    }

    @Test
    void shouldRejectActionContributionConflictingWithTargetModuleAction() {
            StaticModuleDefinitionScanner scanner = scanner(
                    new ConflictingDictionaryCategoryWeb(),
                    new DictionaryItemWebController()
            );

            assertThatThrownBy(scanner::scan)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("action conflicts with target module")
                    .hasMessageContaining("platform.dictionary_category.item_query");
    }

    @Test
    void shouldScanRecordLinkageRuleConfigurationModules() {
            Map<String, StaticModuleDefinition> byAlias = scanner(
                    new RecordGenerationRuleWebController(),
                    new RecordWriteBackRuleWebController()
            ).scan().stream()
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

    @Test
    void shouldAssembleWorkflowActionsFromStaticModuleCapabilities() {
            StaticModuleDefinition definition = scanner(new WorkflowEnabledWeb()).scan().getFirst();

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

    @Test
    void shouldScanWorkflowAdminManagementActions() {
            StaticModuleDefinition definition = scanner(new WorkflowRuntimeAdminWebController(null)).scan().getFirst();

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

    @Test
    void shouldScanWorkflowConfigurationModules() {
            Map<String, StaticModuleDefinition> byAlias = scanner(
                    new WorkflowDefinitionWebController(
                            mock(net.ximatai.muyun.spring.platform.module.PlatformModuleService.class),
                            mock(WorkflowPublishFacade.class)),
                    new WorkflowVersionWebController(mock(WorkflowDefinitionService.class))
            ).scan().stream()
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

    @Test
    void shouldUseLastModuleSegmentAsStaticEntityAlias() {
            StaticModuleDefinition definition = scanner(new MultiSegmentModuleWeb(new MultiSegmentModuleService()))
                    .scan().getFirst();

            assertThat(definition.moduleAlias()).isEqualTo("platform.workflow.definition");
            assertThat(definition.entities()).singleElement()
                    .satisfies(entity -> assertThat(entity.alias()).isEqualTo("definition"));
    }

    @Test
    void shouldScanSnakeCaseWebScopeForCamelCaseStaticAlias() {
            StaticModuleDefinition definition = scanner(new PlatformFieldTypeWebController()).scan().getFirst();

            assertThat(definition.moduleAlias()).isEqualTo("platform.field_type");
            assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactly("menu", "create", "view", "update", "delete", "query",
                            "sort", "enable", "disable");
    }

    @Test
    void shouldScanNestedResourceControllerActionsFromInheritedEndpoints() {
            StaticModuleDefinition definition = scanner(new PlatformUiSetWebController()).scan().getFirst();

            assertThat(definition.moduleAlias()).isEqualTo("platform.ui_set");
            assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactlyInAnyOrder("query", "view", "create", "update", "delete",
                            "enable", "disable", "sort");
    }

    @Test
    void shouldScanFieldUiTypeNestedConfigurationActions() {
            Map<String, StaticModuleDefinition> byAlias = scanner(
                    new PlatformFieldUiTypeAttributeWebController(),
                    new PlatformFieldUiTypeFieldMappingWebController()
            ).scan().stream()
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

    @Test
    void shouldRegisterPageConfigPublishActionsAsRecordActions() {
            StaticModuleDefinition definition = scanner(new PlatformPageConfigPublishWebController()).scan().getFirst();

            assertThat(definition.moduleAlias()).isEqualTo("platform.page_config_publish");
            assertThat(definition.actions()).extracting(StaticModuleActionDefinition::actionCode)
                    .containsExactlyInAnyOrder("publishUiConfig", "unpublishUiConfig",
                            "publishQueryTemplate", "unpublishQueryTemplate");
            assertThat(definition.actions()).allSatisfy(action ->
                    assertThat(action.actionLevel()).isEqualTo(EntityActionLevel.RECORD));
    }

    @Test
    void shouldRegisterLowCodeGovernanceActionsAsStaticModuleActions() {
            StaticModuleDefinition definition = scanner(new LowCodeGovernanceWebController(
                    mock(LowCodeModuleConfigArchiveFacade.class),
                    mock(LowCodeModuleHealthService.class),
                    mock(LowCodeModulePackageExchangeService.class),
                    mock(LowCodeModulePackageImportService.class),
                    mock(LowCodeModuleTemplateService.class)
            )).scan().getFirst();

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

    @Test
    void shouldCompileStaticServiceModelMeasureUnitFieldsIntoModuleDefinition() {
            StaticMeasureOrderService service = new StaticMeasureOrderService();
            StaticModuleDefinition definition = scanner(new StaticMeasureOrderWeb(service)).scan().getFirst();

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

    private StaticModuleDefinitionScanner scanner(Object... beans) {
        return new StaticModuleDefinitionScanner(List.of(beans));
    }

    private static class TestDictionaryCategoryWebController extends DictionaryCategoryWebController {
        TestDictionaryCategoryWebController(DictionaryCategoryService service) {
            this.service = service;
        }
    }

    private static class TestDictionaryItemWebController extends DictionaryItemWebController {
        TestDictionaryItemWebController(DictionaryItemService service) {
            this.service = service;
        }
    }

    @PlatformStaticModule(application = "sales", alias = "sales.contract", title = "合同",
            capabilities = EntityCapability.APPROVAL)
    static class WorkflowEnabledWeb {
    }

    @PlatformStaticModule(application = "platform", alias = "platform.dictionary_category", title = "字典管理")
    static class ConflictingDictionaryCategoryWeb {
        @CustomActionEndpoint("item_query")
        public void itemQuery() {
        }
    }

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

    @PlatformStaticModule(application = "platform", alias = "platform.workflow.definition", title = "流程定义")
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
