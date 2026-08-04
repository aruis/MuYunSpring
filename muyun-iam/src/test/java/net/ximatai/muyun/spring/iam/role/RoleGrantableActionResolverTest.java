package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleGrantableActionResolverTest {
    @Test
    void shouldNotFallBackToStaticDefaultsWhenGovernanceDisablesAllRegisteredActions() {
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        PlatformModuleActionService moduleActionService = mock(PlatformModuleActionService.class);
        PlatformModule module = new PlatformModule();
        module.setAlias("iam.user");
        module.setModuleKind(ModuleKind.STATIC);
        PlatformModuleAction action = new PlatformModuleAction();
        action.setModuleAlias("iam.user");
        action.setActionCode("query");
        action.setActionAuth(true);
        action.setActionAuthOverride(false);
        action.setEnabled(true);
        when(moduleService.resolveVisibleModule("iam.user")).thenReturn(module);
        when(moduleActionService.listByModuleAliases(List.of("iam.user"))).thenReturn(List.of(action));
        RoleGrantableActionResolver resolver = new RoleGrantableActionResolver(moduleService, moduleActionService);

        assertThat(resolver.resolve(List.of("iam.user"))).isEmpty();
    }
}
