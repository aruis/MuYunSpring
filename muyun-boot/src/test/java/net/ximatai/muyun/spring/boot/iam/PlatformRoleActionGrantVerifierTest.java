package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformRoleActionGrantVerifierTest {
    @Test
    void shouldRejectMissingModule() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformRoleActionGrantVerifier verifier = new PlatformRoleActionGrantVerifier(
                moduleService,
                mock(PlatformModuleActionService.class)
        );

        assertThatThrownBy(() -> verifier.requireGrantable("sales.contract", "query"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("existing module");
    }

    @Test
    void shouldAllowStandardPlatformActionForStaticModuleWithoutRegisteredActions() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService moduleActionService = mock(PlatformModuleActionService.class);
        when(moduleService.resolveVisibleModule("sales.contract"))
                .thenReturn(module("sales.contract", ModuleKind.STATIC));
        when(moduleActionService.listByModuleAliases(List.of("sales.contract"))).thenReturn(List.of());
        PlatformRoleActionGrantVerifier verifier = new PlatformRoleActionGrantVerifier(
                moduleService,
                moduleActionService
        );

        verifier.requireGrantable("sales.contract", "query");
    }

    @Test
    void shouldRejectUndeclaredPlatformActionForStaticModuleWithoutRegisteredActions() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService moduleActionService = mock(PlatformModuleActionService.class);
        when(moduleService.resolveVisibleModule("sales.contract"))
                .thenReturn(module("sales.contract", ModuleKind.STATIC));
        when(moduleActionService.listByModuleAliases(List.of("sales.contract"))).thenReturn(List.of());
        PlatformRoleActionGrantVerifier verifier = new PlatformRoleActionGrantVerifier(
                moduleService,
                moduleActionService,
                new StaticModuleActionRegistry(Map.of("sales.contract", List.of(PlatformAction.QUERY)))
        );

        assertThatThrownBy(() -> verifier.requireGrantable("sales.contract", "delete"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("configured module action");
        verifier.requireGrantable("sales.contract", "query");
    }

    @Test
    void shouldAllowRegisteredModuleAction() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService moduleActionService = mock(PlatformModuleActionService.class);
        when(moduleService.resolveVisibleModule("iam.user"))
                .thenReturn(module("iam.user", ModuleKind.STATIC));
        when(moduleActionService.listByModuleAliases(List.of("iam.user")))
                .thenReturn(List.of(moduleAction("changePassword", true)));
        PlatformRoleActionGrantVerifier verifier = new PlatformRoleActionGrantVerifier(
                moduleService,
                moduleActionService,
                new StaticModuleActionRegistry(Map.of("iam.user", List.of()))
        );

        verifier.requireGrantable("iam.user", "changePassword");
    }

    @Test
    void shouldRejectStaticActionMissingFromRegisteredModuleActions() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService moduleActionService = mock(PlatformModuleActionService.class);
        when(moduleService.resolveVisibleModule("iam.user"))
                .thenReturn(module("iam.user", ModuleKind.STATIC));
        when(moduleActionService.listByModuleAliases(List.of("iam.user")))
                .thenReturn(List.of(moduleAction("query", true)));
        PlatformRoleActionGrantVerifier verifier = new PlatformRoleActionGrantVerifier(
                moduleService,
                moduleActionService,
                new StaticModuleActionRegistry(Map.of("iam.user", List.of(PlatformAction.TREE)))
        );

        assertThatThrownBy(() -> verifier.requireGrantable("iam.user", "tree"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("configured module action");

        verify(moduleActionService).listByModuleAliases(List.of("iam.user"));
    }

    @Test
    void shouldRequireRegisteredActionForDynamicModuleEvenWhenActionIsPlatformStandard() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService moduleActionService = mock(PlatformModuleActionService.class);
        when(moduleService.resolveVisibleModule("sales.contract"))
                .thenReturn(module("sales.contract", ModuleKind.DYNAMIC));
        when(moduleActionService.listByModuleAliases(List.of("sales.contract"))).thenReturn(List.of());
        PlatformRoleActionGrantVerifier verifier = new PlatformRoleActionGrantVerifier(
                moduleService,
                moduleActionService
        );

        assertThatThrownBy(() -> verifier.requireGrantable("sales.contract", "query"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("configured module action");
    }

    @Test
    void shouldRejectRegisteredActionThatDoesNotParticipateInActionAuth() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService moduleActionService = mock(PlatformModuleActionService.class);
        when(moduleService.resolveVisibleModule("sales.contract"))
                .thenReturn(module("sales.contract", ModuleKind.DYNAMIC));
        when(moduleActionService.listByModuleAliases(List.of("sales.contract")))
                .thenReturn(List.of(moduleAction("submit", false)));
        PlatformRoleActionGrantVerifier verifier = new PlatformRoleActionGrantVerifier(moduleService, moduleActionService);

        assertThatThrownBy(() -> verifier.requireGrantable("sales.contract", "submit"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("configured module action");
    }

    @Test
    void shouldNotUseStaticFallbackWhenRegisteredActionsExist() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService moduleActionService = mock(PlatformModuleActionService.class);
        when(moduleService.resolveVisibleModule("iam.user"))
                .thenReturn(module("iam.user", ModuleKind.STATIC));
        when(moduleActionService.listByModuleAliases(List.of("iam.user")))
                .thenReturn(List.of(moduleAction("query", true)));
        StaticModuleActionRegistry registry = mock(StaticModuleActionRegistry.class);
        PlatformRoleActionGrantVerifier verifier = new PlatformRoleActionGrantVerifier(
                moduleService,
                moduleActionService,
                registry
        );

        assertThatThrownBy(() -> verifier.requireGrantable("iam.user", "tree"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("configured module action");

        verify(registry, never()).isGrantable("iam.user", PlatformAction.TREE);
    }

    private PlatformModuleAction moduleAction(String actionCode, boolean actionAuth) {
        PlatformModuleAction action = new PlatformModuleAction();
        action.setModuleAlias("iam.user");
        action.setActionCode(actionCode);
        action.setActionAuth(actionAuth);
        action.setEnabled(Boolean.TRUE);
        return action;
    }

    private PlatformModule module(String moduleAlias, ModuleKind moduleKind) {
        PlatformModule module = new PlatformModule();
        module.setAlias(moduleAlias);
        module.setModuleKind(moduleKind);
        return module;
    }
}
