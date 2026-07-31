package net.ximatai.muyun.spring.web.realtime;

import net.ximatai.muyun.spring.common.identity.CurrentUser;

import java.security.Principal;

public record CurrentUserPrincipal(CurrentUser currentUser, String token, String loginSessionId) implements Principal {
    public CurrentUserPrincipal(CurrentUser currentUser, String token) {
        this(currentUser, token, null);
    }

    public CurrentUserPrincipal {
        if (currentUser == null) {
            throw new IllegalArgumentException("currentUser must not be null");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        token = token.trim();
        loginSessionId = loginSessionId == null || loginSessionId.isBlank() ? null : loginSessionId.trim();
    }

    @Override
    public String getName() {
        return currentUser.userId();
    }
}
