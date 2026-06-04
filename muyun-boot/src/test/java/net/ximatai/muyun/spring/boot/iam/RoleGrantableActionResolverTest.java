package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.iam.role.GrantableAction;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataAction;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataActionService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
