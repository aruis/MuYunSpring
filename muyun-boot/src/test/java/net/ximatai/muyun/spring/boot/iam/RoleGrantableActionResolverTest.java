package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.iam.role.GrantableAction;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataAction;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataActionService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleGrantableActionResolverTest {
    @Test
    void shouldKeepCanonicalPermissionActionWhenConfiguredActionInheritsPlatformPermission() {
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        ModuleMetadataActionService actionService = mock(ModuleMetadataActionService.class);
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        when(moduleService.resolveVisibleModule("sales.contract")).thenReturn(module("sales.contract", ModuleKind.STATIC));
        ModuleMetadataRelation relation = new ModuleMetadataRelation();
        relation.setId("rel-1");
        when(relationService.list(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(relation));
        when(actionService.listByRelationIds(List.of("rel-1"))).thenReturn(List.of(
                action("submit", "Submit", "query", true, true),
                action("approve", "Approve", null, true, true)
        ));
        RoleGrantableActionResolver resolver = new RoleGrantableActionResolver(moduleService, relationService, actionService);

        List<GrantableAction> actions = resolver.resolve(List.of("sales.contract"));

        assertThat(actions).extracting(GrantableAction::permissionActionCode)
                .contains("view", "delete", "sort", "enable", "approve");
        assertThat(actions).filteredOn(action -> "view".equals(action.permissionActionCode()))
                .singleElement()
                .satisfies(action -> {
                    assertThat(action.actionCode()).isEqualTo("view");
                    assertThat(action.title()).isEqualTo("View");
                    assertThat(action.dataAuth()).isTrue();
                });
        assertThat(actions).filteredOn(action -> "approve".equals(action.permissionActionCode()))
                .singleElement()
                .satisfies(action -> {
                    assertThat(action.actionCode()).isEqualTo("approve");
                    assertThat(action.title()).isEqualTo("Approve");
                    assertThat(action.dataAuth()).isTrue();
                });
    }

    @Test
    void shouldIgnoreDisabledOrNonActionAuthConfiguredActions() {
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        ModuleMetadataActionService actionService = mock(ModuleMetadataActionService.class);
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        when(moduleService.resolveVisibleModule("sales.contract")).thenReturn(module("sales.contract", ModuleKind.STATIC));
        ModuleMetadataRelation relation = new ModuleMetadataRelation();
        relation.setId("rel-1");
        when(relationService.list(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(relation));
        when(actionService.listByRelationIds(List.of("rel-1"))).thenReturn(List.of(
                action("silent", "Silent", null, false, true),
                action("disabledSubmit", "Disabled Submit", null, true, false)
        ));
        RoleGrantableActionResolver resolver = new RoleGrantableActionResolver(moduleService, relationService, actionService);

        List<GrantableAction> actions = resolver.resolve(List.of("sales.contract"));

        assertThat(actions).noneMatch(action -> "silent".equals(action.actionCode()));
        assertThat(actions).noneMatch(action -> "disabledSubmit".equals(action.actionCode()));
    }

    @Test
    void shouldUseConfiguredActionsOnlyForDynamicModules() {
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        ModuleMetadataActionService actionService = mock(ModuleMetadataActionService.class);
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        when(moduleService.resolveVisibleModule("sales.contract")).thenReturn(module("sales.contract", ModuleKind.DYNAMIC));
        ModuleMetadataRelation relation = new ModuleMetadataRelation();
        relation.setId("rel-1");
        when(relationService.list(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(relation));
        when(actionService.listByRelationIds(List.of("rel-1"))).thenReturn(List.of(
                action("query", "Query", "view", true, true)
        ));
        RoleGrantableActionResolver resolver = new RoleGrantableActionResolver(moduleService, relationService, actionService);

        List<GrantableAction> actions = resolver.resolve(List.of("sales.contract", "missing.module"));

        assertThat(actions).extracting(GrantableAction::actionCode).containsExactly("query");
        assertThat(actions).extracting(GrantableAction::permissionActionCode).containsExactly("view");
    }

    @Test
    void shouldFilterStaticPlatformActionsByDeclaredModuleActions() {
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        ModuleMetadataActionService actionService = mock(ModuleMetadataActionService.class);
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        when(moduleService.resolveVisibleModule("sales.contract")).thenReturn(module("sales.contract", ModuleKind.STATIC));
        when(relationService.list(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        RoleGrantableActionResolver resolver = new RoleGrantableActionResolver(
                moduleService,
                relationService,
                actionService,
                new StaticModuleActionRegistry(Map.of("sales.contract", List.of(PlatformAction.VIEW, PlatformAction.DELETE)))
        );

        List<GrantableAction> actions = resolver.resolve(List.of("sales.contract"));

        assertThat(actions).extracting(GrantableAction::actionCode)
                .containsExactly("view", "delete");
    }

    @Test
    void shouldExposeRegisteredModuleActionsForPermissionGranting() {
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        ModuleMetadataActionService actionService = mock(ModuleMetadataActionService.class);
        PlatformModuleActionService moduleActionService = mock(PlatformModuleActionService.class);
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        when(moduleService.resolveVisibleModule("iam.user")).thenReturn(module("iam.user", ModuleKind.STATIC));
        when(moduleActionService.listByModuleAliases(List.of("iam.user")))
                .thenReturn(List.of(moduleAction("changePassword", "修改密码", false)));
        when(relationService.list(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        RoleGrantableActionResolver resolver = new RoleGrantableActionResolver(
                moduleService,
                relationService,
                actionService,
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
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        ModuleMetadataActionService actionService = mock(ModuleMetadataActionService.class);
        PlatformModuleActionService moduleActionService = mock(PlatformModuleActionService.class);
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        when(moduleService.resolveVisibleModule("iam.user")).thenReturn(module("iam.user", ModuleKind.STATIC));
        when(moduleActionService.listByModuleAliases(List.of("iam.user")))
                .thenReturn(List.of(
                        moduleAction("query", "查询", true),
                        moduleAction("changePassword", "修改密码", true)
                ));
        when(relationService.list(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        RoleGrantableActionResolver resolver = new RoleGrantableActionResolver(
                moduleService,
                relationService,
                actionService,
                moduleActionService,
                new StaticModuleActionRegistry(Map.of("iam.user", List.of(PlatformAction.TREE, PlatformAction.REFERENCE)))
        );

        List<GrantableAction> actions = resolver.resolve(List.of("iam.user"));

        assertThat(actions).extracting(GrantableAction::actionCode)
                .containsExactly("query", "changePassword");
        assertThat(actions).extracting(GrantableAction::actionCode)
                .doesNotContain("tree", "reference");
    }

    private PlatformModule module(String moduleAlias, ModuleKind moduleKind) {
        PlatformModule module = new PlatformModule();
        module.setAlias(moduleAlias);
        module.setModuleKind(moduleKind);
        return module;
    }

    private ModuleMetadataAction action(String actionCode,
                                        String title,
                                        String inheritActionCode,
                                        boolean actionAuth,
                                        boolean enabled) {
        ModuleMetadataAction action = new ModuleMetadataAction();
        action.setActionCode(actionCode);
        action.setTitle(title);
        action.setAuthInheritActionCode(inheritActionCode);
        action.setActionAuth(actionAuth);
        action.setDataAuth(true);
        action.setEnabled(enabled);
        return action;
    }

    private PlatformModuleAction moduleAction(String actionCode, String title, boolean dataAuth) {
        PlatformModuleAction action = new PlatformModuleAction();
        action.setModuleAlias("iam.user");
        action.setActionCode(actionCode);
        action.setPermissionActionCode(actionCode);
        action.setTitle(title);
        action.setActionAuth(Boolean.TRUE);
        action.setDataAuth(dataAuth);
        action.setEnabled(Boolean.TRUE);
        return action;
    }
}
