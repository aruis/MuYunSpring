package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.iam.role.GrantableAction;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleGrantableActionResolverTest {
    @Test
    void shouldFallbackToStaticRegistryWhenModuleHasNoRegisteredActions() {
        PlatformModuleActionService moduleActionService = mock(PlatformModuleActionService.class);
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        when(moduleService.resolveVisibleModule("sales.contract")).thenReturn(module("sales.contract", ModuleKind.STATIC));
        when(moduleActionService.listByModuleAliases(List.of("sales.contract"))).thenReturn(List.of());
        RoleGrantableActionResolver resolver = new RoleGrantableActionResolver(
                moduleService,
                moduleActionService,
                new StaticModuleActionRegistry(Map.of("sales.contract", List.of(PlatformAction.VIEW, PlatformAction.DELETE)))
        );

        List<GrantableAction> actions = resolver.resolve(List.of("sales.contract"));

        assertThat(actions).extracting(GrantableAction::actionCode)
                .containsExactly("view", "delete");
    }

    @Test
    void shouldExposeRegisteredModuleActionsForPermissionGranting() {
        PlatformModuleActionService moduleActionService = mock(PlatformModuleActionService.class);
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        when(moduleService.resolveVisibleModule("iam.user")).thenReturn(module("iam.user", ModuleKind.STATIC));
        when(moduleActionService.listByModuleAliases(List.of("iam.user")))
                .thenReturn(List.of(moduleAction("changePassword", "changePassword", "修改密码", false)));
        RoleGrantableActionResolver resolver = new RoleGrantableActionResolver(
                moduleService,
                moduleActionService,
                new StaticModuleActionRegistry(Map.of("iam.user", List.of()))
        );

        List<GrantableAction> actions = resolver.resolve(List.of("iam.user"));

        assertThat(actions).filteredOn(action -> "changePassword".equals(action.actionCode()))
                .singleElement()
                .satisfies(action -> {
                    assertThat(action.permissionActionCode()).isEqualTo("changePassword");
                    assertThat(action.title()).isEqualTo("修改密码");
                    assertThat(action.actionAuth()).isTrue();
                    assertThat(action.dataAuth()).isFalse();
                });
    }

    @Test
    void shouldUseRegisteredModuleActionsAsStaticModuleGrantableSurface() {
        PlatformModuleActionService moduleActionService = mock(PlatformModuleActionService.class);
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        when(moduleService.resolveVisibleModule("iam.user")).thenReturn(module("iam.user", ModuleKind.STATIC));
        when(moduleActionService.listByModuleAliases(List.of("iam.user")))
                .thenReturn(List.of(
                        moduleAction("query", "view", "查询", true),
                        moduleAction("changePassword", "changePassword", "修改密码", true)
                ));
        RoleGrantableActionResolver resolver = new RoleGrantableActionResolver(
                moduleService,
                moduleActionService,
                new StaticModuleActionRegistry(Map.of("iam.user", List.of(PlatformAction.TREE, PlatformAction.REFERENCE)))
        );

        List<GrantableAction> actions = resolver.resolve(List.of("iam.user"));

        assertThat(actions).extracting(GrantableAction::actionCode)
                .containsExactly("query", "changePassword");
        assertThat(actions).extracting(GrantableAction::permissionActionCode)
                .containsExactly("view", "changePassword");
        assertThat(actions).extracting(GrantableAction::actionCode)
                .doesNotContain("tree", "reference");
    }

    @Test
    void shouldIgnoreDisabledOrNonActionAuthRegisteredActions() {
        PlatformModuleActionService moduleActionService = mock(PlatformModuleActionService.class);
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        when(moduleService.resolveVisibleModule("sales.contract")).thenReturn(module("sales.contract", ModuleKind.DYNAMIC));
        PlatformModuleAction silent = moduleAction("silent", "silent", "Silent", false);
        silent.setActionAuth(Boolean.FALSE);
        PlatformModuleAction disabled = moduleAction("disabledSubmit", "disabledSubmit", "Disabled Submit", true);
        disabled.setEnabled(Boolean.FALSE);
        when(moduleActionService.listByModuleAliases(List.of("sales.contract"))).thenReturn(List.of(silent, disabled));
        RoleGrantableActionResolver resolver = new RoleGrantableActionResolver(moduleService, moduleActionService);

        List<GrantableAction> actions = resolver.resolve(List.of("sales.contract"));

        assertThat(actions).isEmpty();
    }

    @Test
    void shouldUseModuleActionsOnlyForDynamicModules() {
        PlatformModuleActionService moduleActionService = mock(PlatformModuleActionService.class);
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        when(moduleService.resolveVisibleModule("sales.contract")).thenReturn(module("sales.contract", ModuleKind.DYNAMIC));
        when(moduleActionService.listByModuleAliases(List.of("sales.contract")))
                .thenReturn(List.of(moduleAction("query", "view", "Query", true)));
        RoleGrantableActionResolver resolver = new RoleGrantableActionResolver(moduleService, moduleActionService);

        List<GrantableAction> actions = resolver.resolve(List.of("sales.contract", "missing.module"));

        assertThat(actions).extracting(GrantableAction::actionCode).containsExactly("query");
        assertThat(actions).extracting(GrantableAction::permissionActionCode).containsExactly("view");
    }

    private PlatformModule module(String moduleAlias, ModuleKind moduleKind) {
        PlatformModule module = new PlatformModule();
        module.setAlias(moduleAlias);
        module.setModuleKind(moduleKind);
        return module;
    }

    private PlatformModuleAction moduleAction(String actionCode, String permissionActionCode, String title, boolean dataAuth) {
        PlatformModuleAction action = new PlatformModuleAction();
        action.setModuleAlias("iam.user");
        action.setActionCode(actionCode);
        action.setPermissionActionCode(permissionActionCode);
        action.setTitle(title);
        action.setActionAuth(Boolean.TRUE);
        action.setDataAuth(dataAuth);
        action.setEnabled(Boolean.TRUE);
        return action;
    }
}
