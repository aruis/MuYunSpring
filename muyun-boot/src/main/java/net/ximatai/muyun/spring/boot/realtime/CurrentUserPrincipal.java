package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.common.identity.CurrentUser;

import java.security.Principal;

public record CurrentUserPrincipal(CurrentUser currentUser, String token) implements Principal {
    public CurrentUserPrincipal {
        if (currentUser == null) {
            throw new IllegalArgumentException("currentUser must not be null");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        token = token.trim();
    }

    @Override
    public String getName() {
        return currentUser.userId();
    }
}
