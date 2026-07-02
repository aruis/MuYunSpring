package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.ability.initialdata.InitialDataAbility;
import net.ximatai.muyun.spring.boot.platform.InitialDataBootstrapTask;
import net.ximatai.muyun.spring.boot.platform.PlatformBootstrapRunner;
import net.ximatai.muyun.spring.boot.platform.PlatformBootstrapTask;
import net.ximatai.muyun.spring.common.di.ObjectProviders;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserProvider;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclarationProvider;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataExecutor;
import net.ximatai.muyun.spring.platform.menu.SystemMenuSchemeAccessPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MuYunSpringIdentityConfigurationTest {
    private final MuYunSpringIdentityConfiguration configuration = new MuYunSpringIdentityConfiguration();

    @Test
    void shouldUseTenantServiceAsActiveTenantVerifier() {
        TenantService tenantService = mock(TenantService.class);

        ActiveTenantVerifier verifier = configuration.activeTenantVerifier(tenantService);

        assertThat(verifier).isSameAs(tenantService);
    }

    @Test
    void shouldFallbackCurrentUserProviderToEmptyWhenUserSessionServiceIsMissing() {
        CurrentUserProvider provider = configuration.currentUserProvider(ObjectProviders.of((UserSessionService) null));

        assertThat(provider.currentUser()).isEmpty();
    }

    @Test
    void shouldCreateBearerTokenCurrentUserProviderWhenUserSessionServiceExists() {
        CurrentUserProvider provider = configuration.currentUserProvider(
                ObjectProviders.of(mock(UserSessionService.class))
        );

        assertThat(provider.currentUser()).isEqualTo(Optional.empty());
    }

    @Test
    void shouldNotFallbackTenantUsersToSystemMenuSchemeByDefault() {
        SystemMenuSchemeAccessPolicy policy = configuration.systemMenuSchemeAccessPolicy();

        assertThat(policy.canUseSystemMenuScheme(CurrentUser.tenantUser(
                UserAccountService.PLATFORM_SUPER_ADMIN_USER_ID,
                UserAccountService.PLATFORM_SUPER_ADMIN_USERNAME,
                "platform"))).isFalse();
        assertThat(policy.canUseSystemMenuScheme(CurrentUser.tenantUser(
                UserAccountService.PLATFORM_SUPER_ADMIN_USER_ID,
                UserAccountService.PLATFORM_SUPER_ADMIN_USERNAME,
                "tenant-a"))).isFalse();
    }

    @Test
    void shouldComposeInitialDataExecutorFromAbilitiesAndProviders() {
        InitialDataAbility<?> ability = mock(InitialDataAbility.class);
        InitialDataDeclarationProvider provider = mock(InitialDataDeclarationProvider.class);

        InitialDataExecutor executor = configuration.initialDataExecutor(
                ObjectProviders.of(List.of(ability)),
                ObjectProviders.of(List.of(provider))
        );

        assertThat(executor).isNotNull();
    }

    @Test
    void shouldCreateBootstrapRunnerAndInitialDataTask() {
        PlatformBootstrapTask task = mock(PlatformBootstrapTask.class);
        InitialDataExecutor executor = mock(InitialDataExecutor.class);

        PlatformBootstrapRunner runner = configuration.platformBootstrapRunner(ObjectProviders.of(List.of(task)));
        InitialDataBootstrapTask initialDataTask = configuration.initialDataBootstrapTask(executor);

        assertThat(runner).isNotNull();
        assertThat(initialDataTask.order()).isEqualTo(100);
        assertThat(initialDataTask.name()).isEqualTo("platform.initial-data");
    }
}
