package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
    void shouldRegisterWorkflowActionMetadataForStaticModuleCapabilities() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService actionService = mock(PlatformModuleActionService.class);
        when(moduleService.select("sales.contract")).thenReturn(null);
        when(actionService.findByModuleAliasAndActionCode("sales.contract", "submitWorkflow")).thenReturn(null);
        StaticModuleDefinitionRegistrar registrar = new StaticModuleDefinitionRegistrar(
                moduleService,
                actionService,
                List.of(new StaticModuleDefinition(
                        "sales",
                        "sales.contract",
                        "合同",
                        null,
                        List.of(StaticModuleActionDefinition.workflowAction("submitWorkflow", "发起流程"))
                ))
        );

        registrar.registerAll();

        ArgumentCaptor<PlatformModuleAction> actionCaptor = ArgumentCaptor.forClass(PlatformModuleAction.class);
        verify(actionService).insert(actionCaptor.capture());
        assertThat(actionCaptor.getValue()).satisfies(action -> {
            assertThat(action.getModuleAlias()).isEqualTo("sales.contract");
            assertThat(action.getActionCode()).isEqualTo("submitWorkflow");
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
                List.of(new StaticModuleDefinition(
                        "iam",
                        "iam.user",
                        "用户管理",
                        null,
                        List.of(
                                StaticModuleActionDefinition.platformAction(PlatformAction.MENU),
                                StaticModuleActionDefinition.recordAction("changePassword", "修改密码")
                        )
                ))
        );

        registrar.registerAll();

        ArgumentCaptor<PlatformModule> moduleCaptor = ArgumentCaptor.forClass(PlatformModule.class);
        verify(moduleService).insert(moduleCaptor.capture());
        assertThat(moduleCaptor.getValue()).satisfies(module -> {
            assertThat(module.getAlias()).isEqualTo("iam.user");
            assertThat(module.getApplicationAlias()).isEqualTo("iam");
            assertThat(module.getTitle()).isEqualTo("用户管理");
            assertThat(module.getModuleKind()).isEqualTo(ModuleKind.STATIC);
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

    private StaticModuleDefinition definition(String moduleAlias, List<StaticModuleActionDefinition> actions) {
        return new StaticModuleDefinition("iam", moduleAlias, "用户管理", null, actions);
    }
}
