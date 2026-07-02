package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BearerTokenCurrentUserProviderTest {
    @AfterEach
    void tearDown() {
        BearerTokenCurrentUserProvider.useAuthorizationHeader(null).close();
    }

    @Test
    void shouldResolveCurrentUserFromBearerTokenHeader() {
        CurrentUser currentUser = CurrentUser.tenantUser("user-1", "alice", "tenant-a", "org-1");
        UserSessionService sessionService = mock(UserSessionService.class);
        when(sessionService.currentUser("token-1")).thenReturn(Optional.of(currentUser));
        BearerTokenCurrentUserProvider provider = new BearerTokenCurrentUserProvider(sessionService);

        try (BearerTokenCurrentUserProvider.Scope ignored =
                     BearerTokenCurrentUserProvider.useAuthorizationHeader("Bearer token-1")) {
            assertThat(provider.currentUser()).contains(currentUser);
        }
    }

    @Test
    void shouldIgnoreMissingOrNonBearerAuthorizationHeader() {
        UserSessionService sessionService = mock(UserSessionService.class);
        when(sessionService.currentUser(null)).thenReturn(Optional.empty());
        BearerTokenCurrentUserProvider provider = new BearerTokenCurrentUserProvider(sessionService);

        assertThat(provider.currentUser()).isEmpty();

        try (BearerTokenCurrentUserProvider.Scope ignored =
                     BearerTokenCurrentUserProvider.useAuthorizationHeader("Basic token-1")) {
            assertThat(provider.currentUser()).isEmpty();
        }
    }

    @Test
    void shouldRestorePreviousAuthorizationHeaderWhenScopeCloses() {
        CurrentUser outerUser = CurrentUser.tenantUser("user-1", "alice", "tenant-a", "org-1");
        CurrentUser innerUser = CurrentUser.tenantUser("user-2", "bob", "tenant-b", "org-2");
        UserSessionService sessionService = mock(UserSessionService.class);
        when(sessionService.currentUser("outer-token")).thenReturn(Optional.of(outerUser));
        when(sessionService.currentUser("inner-token")).thenReturn(Optional.of(innerUser));
        BearerTokenCurrentUserProvider provider = new BearerTokenCurrentUserProvider(sessionService);

        try (BearerTokenCurrentUserProvider.Scope outer =
                     BearerTokenCurrentUserProvider.useAuthorizationHeader("Bearer outer-token")) {
            assertThat(provider.currentUser()).contains(outerUser);
            try (BearerTokenCurrentUserProvider.Scope inner =
                         BearerTokenCurrentUserProvider.useAuthorizationHeader("Bearer inner-token")) {
                assertThat(provider.currentUser()).contains(innerUser);
            }
            assertThat(provider.currentUser()).contains(outerUser);
        }
    }
}
