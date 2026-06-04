package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.tenant.TenantContext;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StaticModuleDefinitionRegistrarTest {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldRegisterStaticModuleAndActionsInSystemScope() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService actionService = mock(PlatformModuleActionService.class);
        when(moduleService.select("iam.user")).thenReturn(null);
        when(actionService.findByModuleAliasAndActionCode("iam.user", "changePassword")).thenReturn(null);
        StaticModuleDefinitionRegistrar registrar = new StaticModuleDefinitionRegistrar(
                moduleService,
                actionService,
                List.of(new StaticModuleDefinition(
                        "iam",
                        "iam.user",
                        "用户管理",
                        null,
                        List.of(StaticModuleActionDefinition.recordAction("changePassword", "修改密码"))
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
        verify(actionService).insert(actionCaptor.capture());
        assertThat(actionCaptor.getValue()).satisfies(action -> {
            assertThat(action.getModuleAlias()).isEqualTo("iam.user");
            assertThat(action.getActionCode()).isEqualTo("changePassword");
            assertThat(action.getPermissionActionCode()).isEqualTo("changePassword");
            assertThat(action.getTitle()).isEqualTo("修改密码");
            assertThat(action.getSystemManaged()).isTrue();
            assertThat(action.getEnabled()).isTrue();
        });
    }
}
