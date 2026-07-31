package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataExecutor;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataBootstrapTask;
import net.ximatai.muyun.spring.platform.runtime.PlatformBootstrapTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StaticModuleDefinitionRegistrarTest {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldBePlatformBootstrapTaskBeforeInitialData() {
        StaticModuleDefinitionRegistrar registrar = new StaticModuleDefinitionRegistrar(
                mock(PlatformModuleService.class),
                mock(PlatformModuleActionService.class),
                List.of()
        );

        assertThat(registrar).isInstanceOf(PlatformBootstrapTask.class);
        assertThat(registrar).isNotInstanceOf(ApplicationRunner.class);
        assertThat(registrar.order()).isLessThan(new InitialDataBootstrapTask(mock(InitialDataExecutor.class)).order());
    }

    @Test
    void shouldRegisterWorkflowActionMetadataForStaticModuleCapabilities() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService actionService = mock(PlatformModuleActionService.class);
        when(moduleService.select("sales.contract")).thenReturn(null);
        when(actionService.findByModuleAliasAndActionCode("sales.contract", "submitApproval")).thenReturn(null);
        StaticModuleDefinitionRegistrar registrar = new StaticModuleDefinitionRegistrar(
                moduleService,
                actionService,
                List.of(StaticModuleDefinition.builder("sales", "sales.contract", "合同")
                                .parentModuleAlias(null)
                                .actions(List.of(StaticModuleActionDefinition.workflowAction("submitApproval", "提交审批")))
                                .build())
        );

        registrar.registerAll();

        ArgumentCaptor<PlatformModuleAction> actionCaptor = ArgumentCaptor.forClass(PlatformModuleAction.class);
        verify(actionService).insert(actionCaptor.capture());
        assertThat(actionCaptor.getValue()).satisfies(action -> {
            assertThat(action.getModuleAlias()).isEqualTo("sales.contract");
            assertThat(action.getActionCode()).isEqualTo("submitApproval");
            assertThat(action.getCategory()).isEqualTo(EntityActionCategory.WORKFLOW);
            assertThat(action.getExecutorType()).isEqualTo(EntityActionExecutorType.SERVICE);
            assertThat(action.getExecutorKey()).isEqualTo("platform.workflow");
            assertThat(action.getDataAuth()).isFalse();
        });
    }

    @Test
    void shouldRegisterStaticModuleAndActionsInSystemScope() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService actionService = mock(PlatformModuleActionService.class);
        when(moduleService.select("iam.user")).thenReturn(null);
        when(actionService.findByModuleAliasAndActionCode("iam.user", "menu")).thenReturn(null);
        when(actionService.findByModuleAliasAndActionCode("iam.user", "changePassword")).thenReturn(null);
        StaticModuleDefinitionRegistrar registrar = new StaticModuleDefinitionRegistrar(
                moduleService,
                actionService,
                List.of(StaticModuleDefinition.builder("iam", "iam.user", "用户管理")
                                .parentModuleAlias(null)
                                .actions(List.of(
                                StaticModuleActionDefinition.platformAction(PlatformAction.MENU),
                                StaticModuleActionDefinition.recordAction("changePassword", "修改密码")
                        ))
                                .build())
        );

        registrar.registerAll();

        ArgumentCaptor<PlatformModule> moduleCaptor = ArgumentCaptor.forClass(PlatformModule.class);
        verify(moduleService).insert(moduleCaptor.capture());
        assertThat(moduleCaptor.getValue()).satisfies(module -> {
            assertThat(module.getAlias()).isEqualTo("iam.user");
            assertThat(module.getApplicationAlias()).isEqualTo("iam");
            assertThat(module.getTitle()).isEqualTo("用户管理");
            assertThat(module.getModuleKind()).isEqualTo(ModuleKind.STATIC);
            assertThat(module.getEntryType()).isEqualTo(ModuleEntryType.MODULE);
            assertThat(module.getSystemManaged()).isTrue();
        });
        ArgumentCaptor<PlatformModuleAction> actionCaptor = ArgumentCaptor.forClass(PlatformModuleAction.class);
        verify(actionService, times(2)).insert(actionCaptor.capture());
        assertThat(actionCaptor.getAllValues()).filteredOn(action -> "menu".equals(action.getActionCode()))
                .singleElement()
                .satisfies(action -> {
                    assertThat(action.getModuleAlias()).isEqualTo("iam.user");
                    assertThat(action.getPermissionActionCode()).isEqualTo("menu");
                    assertThat(action.getDataAuth()).isFalse();
                    assertThat(action.getSystemManaged()).isTrue();
                    assertThat(action.getEnabled()).isTrue();
                });
        assertThat(actionCaptor.getAllValues()).filteredOn(action -> "changePassword".equals(action.getActionCode()))
                .singleElement()
                .satisfies(action -> {
                    assertThat(action.getModuleAlias()).isEqualTo("iam.user");
                    assertThat(action.getActionCode()).isEqualTo("changePassword");
                    assertThat(action.getPermissionActionCode()).isEqualTo("changePassword");
                    assertThat(action.getTitle()).isEqualTo("修改密码");
                    assertThat(action.getSystemManaged()).isTrue();
                    assertThat(action.getEnabled()).isTrue();
                });
    }

    @Test
    void shouldRegisterStaticModuleEntryOnPlatformModule() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService actionService = mock(PlatformModuleActionService.class);
        when(moduleService.select("iam.organization")).thenReturn(null);
        StaticModuleDefinitionRegistrar registrar = new StaticModuleDefinitionRegistrar(
                moduleService,
                actionService,
                List.of(StaticModuleDefinition.builder("iam", "iam.organization", "机构管理")
                                .parentModuleAlias(null)
                                .entry(ModuleEntryType.ROUTE, "/iam/organizations", null)
                                .capabilities(java.util.Set.of())
                                .actions(List.of())
                                .entities(List.of())
                                .build())
        );

        registrar.registerAll();

        ArgumentCaptor<PlatformModule> moduleCaptor = ArgumentCaptor.forClass(PlatformModule.class);
        verify(moduleService).insert(moduleCaptor.capture());
        assertThat(moduleCaptor.getValue()).satisfies(module -> {
            assertThat(module.getAlias()).isEqualTo("iam.organization");
            assertThat(module.getEntryType()).isEqualTo(ModuleEntryType.ROUTE);
            assertThat(module.getEntryRoute()).isEqualTo("/iam/organizations");
            assertThat(module.getEntryExternalUrl()).isNull();
        });
    }

    @Test
    void shouldDisableStaleSystemManagedStaticModulesWhenScanningAllDefinitions() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService actionService = mock(PlatformModuleActionService.class);
        StaticModuleDefinitionScanner scanner = mock(StaticModuleDefinitionScanner.class);
        when(scanner.scan()).thenReturn(List.of(definition("iam.user",
                List.of(StaticModuleActionDefinition.platformAction(PlatformAction.MENU)))));
        when(moduleService.select("iam.user")).thenReturn(null);
        when(actionService.findByModuleAliasAndActionCode("iam.user", "menu")).thenReturn(null);
        PlatformModule current = module("iam.user");
        PlatformModule stale = module("platform.dictionary_item");
        PlatformModuleAction staleAction = action("action-1", "platform.dictionary_item", "query");
        when(moduleService.listSystemManagedStaticModules()).thenReturn(List.of(current, stale));
        when(actionService.listSystemManagedActions("platform.dictionary_item")).thenReturn(List.of(staleAction));

        StaticModuleDefinitionRegistrar registrar = new StaticModuleDefinitionRegistrar(
                moduleService,
                actionService,
                List.of(),
                List.of(scanner)
        );

        registrar.registerAll();

        verify(actionService).disable("action-1");
        verify(moduleService).disable("platform.dictionary_item");
    }

    @Test
    void shouldRejectDuplicateStaticModuleDefinitions() {
        StaticModuleDefinitionRegistrar registrar = new StaticModuleDefinitionRegistrar(
                mock(PlatformModuleService.class),
                mock(PlatformModuleActionService.class),
                List.of(
                        definition("iam.user", List.of(StaticModuleActionDefinition.recordAction("changePassword", "修改密码"))),
                        definition("iam.user", List.of(StaticModuleActionDefinition.recordAction("resetPassword", "重置密码")))
                )
        );

        assertThatThrownBy(registrar::registerAll)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate static module definition: iam.user");
    }

    @Test
    void shouldRejectDuplicateStaticModuleActionDefinitions() {
        StaticModuleDefinitionRegistrar registrar = new StaticModuleDefinitionRegistrar(
                mock(PlatformModuleService.class),
                mock(PlatformModuleActionService.class),
                List.of(definition("iam.user", List.of(
                        StaticModuleActionDefinition.recordAction("changePassword", "修改密码"),
                        StaticModuleActionDefinition.recordAction("changePassword", "修改密码")
                )))
        );

        assertThatThrownBy(registrar::registerAll)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate static module action definition: iam.user.changePassword");
    }

    @Test
    void shouldRejectStaticModuleReferencingUndeclaredApplication() {
        StaticModuleDefinitionRegistrar registrar = new StaticModuleDefinitionRegistrar(
                mock(PlatformModuleService.class),
                mock(PlatformModuleActionService.class),
                new StaticModuleDefinitionCatalog(List.of(definition("iam.user", List.of()))),
                false,
                new StaticApplicationDefinitionCatalog(List.of(
                        StaticApplicationDefinition.of("platform", "平台能力", 10))));

        assertThatThrownBy(registrar::registerAll)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("iam.user -> iam");
    }

    private StaticModuleDefinition definition(String moduleAlias, List<StaticModuleActionDefinition> actions) {
        return StaticModuleDefinition.builder("iam", moduleAlias, "用户管理")
                       .parentModuleAlias(null)
                       .actions(actions)
                       .build();
    }

    private PlatformModule module(String moduleAlias) {
        PlatformModule module = new PlatformModule();
        module.setAlias(moduleAlias);
        module.setApplicationAlias(moduleAlias.substring(0, moduleAlias.indexOf('.')));
        module.setModuleKind(ModuleKind.STATIC);
        module.setSystemManaged(Boolean.TRUE);
        return module;
    }

    private PlatformModuleAction action(String id, String moduleAlias, String actionCode) {
        PlatformModuleAction action = new PlatformModuleAction();
        action.setId(id);
        action.setModuleAlias(moduleAlias);
        action.setActionCode(actionCode);
        action.setSystemManaged(Boolean.TRUE);
        return action;
    }
}
