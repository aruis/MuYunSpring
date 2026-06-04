package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataAction;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataActionService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
                mock(ModuleMetadataRelationService.class),
                mock(ModuleMetadataActionService.class)
        );

        assertThatThrownBy(() -> verifier.requireGrantable("sales.contract", "query"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("existing module");
    }

    @Test
    void shouldAllowStandardPlatformActionForExistingModule() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        when(moduleService.resolveVisibleModule("sales.contract")).thenReturn(new PlatformModule());
        PlatformRoleActionGrantVerifier verifier = new PlatformRoleActionGrantVerifier(
                moduleService,
                relationService,
                mock(ModuleMetadataActionService.class)
        );

        verifier.requireGrantable("sales.contract", "query");

        verify(relationService, never()).list(any(Criteria.class), any(PageRequest.class));
    }

    @Test
    void shouldAllowConfiguredCustomAction() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        ModuleMetadataActionService actionService = mock(ModuleMetadataActionService.class);
        when(moduleService.resolveVisibleModule("sales.contract")).thenReturn(new PlatformModule());
        when(relationService.list(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(relation("rel-1")));
        when(actionService.listByRelationIds(List.of("rel-1")))
                .thenReturn(List.of(action("submit", true, true)));
        PlatformRoleActionGrantVerifier verifier = new PlatformRoleActionGrantVerifier(
                moduleService,
                relationService,
                actionService
        );

        verifier.requireGrantable("sales.contract", "submit");
    }

    @Test
    void shouldRequireConfiguredActionForDynamicModuleEvenWhenActionIsPlatformStandard() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        ModuleMetadataActionService actionService = mock(ModuleMetadataActionService.class);
        when(moduleService.resolveVisibleModule("sales.contract"))
                .thenReturn(module("sales.contract", ModuleKind.DYNAMIC));
        when(relationService.list(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of());
        PlatformRoleActionGrantVerifier verifier = new PlatformRoleActionGrantVerifier(
                moduleService,
                relationService,
                actionService
        );

        assertThatThrownBy(() -> verifier.requireGrantable("sales.contract", "query"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("configured module action");
    }

    @Test
    void shouldAllowConfiguredPlatformActionForDynamicModule() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        ModuleMetadataActionService actionService = mock(ModuleMetadataActionService.class);
        when(moduleService.resolveVisibleModule("sales.contract"))
                .thenReturn(module("sales.contract", ModuleKind.DYNAMIC));
        when(relationService.list(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(relation("rel-1")));
        when(actionService.listByRelationIds(List.of("rel-1")))
                .thenReturn(List.of(action("query", true, true)));
        PlatformRoleActionGrantVerifier verifier = new PlatformRoleActionGrantVerifier(
                moduleService,
                relationService,
                actionService
        );

        verifier.requireGrantable("sales.contract", "query");
    }

    @Test
    void shouldRejectCustomActionThatDoesNotParticipateInActionAuth() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        ModuleMetadataActionService actionService = mock(ModuleMetadataActionService.class);
        when(moduleService.resolveVisibleModule("sales.contract")).thenReturn(new PlatformModule());
        when(relationService.list(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(relation("rel-1")));
        when(actionService.listByRelationIds(List.of("rel-1")))
                .thenReturn(List.of(action("submit", false, true)));
        PlatformRoleActionGrantVerifier verifier = new PlatformRoleActionGrantVerifier(
                moduleService,
                relationService,
                actionService
        );

        assertThatThrownBy(() -> verifier.requireGrantable("sales.contract", "submit"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("configured module action");
    }

    private ModuleMetadataRelation relation(String id) {
        ModuleMetadataRelation relation = new ModuleMetadataRelation();
        relation.setId(id);
        return relation;
    }

    private ModuleMetadataAction action(String actionCode, Boolean actionAuth, Boolean enabled) {
        ModuleMetadataAction action = new ModuleMetadataAction();
        action.setActionCode(actionCode);
        action.setActionAuth(actionAuth);
        action.setEnabled(enabled);
        return action;
    }

    private PlatformModule module(String moduleAlias, ModuleKind moduleKind) {
        PlatformModule module = new PlatformModule();
        module.setAlias(moduleAlias);
        module.setModuleKind(moduleKind);
        return module;
    }
}
