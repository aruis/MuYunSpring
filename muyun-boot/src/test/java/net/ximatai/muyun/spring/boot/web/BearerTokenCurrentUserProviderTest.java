package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BearerTokenCurrentUserProviderTest {
    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldResolveCurrentUserFromBearerTokenHeader() {
        CurrentUser currentUser = CurrentUser.tenantUser("user-1", "alice", "tenant-a", "org-1");
        UserSessionService sessionService = mock(UserSessionService.class);
        when(sessionService.currentUser("token-1")).thenReturn(Optional.of(currentUser));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        BearerTokenCurrentUserProvider provider = new BearerTokenCurrentUserProvider(sessionService);

        assertThat(provider.currentUser()).contains(currentUser);
    }
}
