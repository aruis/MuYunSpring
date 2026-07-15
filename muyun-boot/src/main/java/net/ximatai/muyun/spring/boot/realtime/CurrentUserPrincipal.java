package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.common.identity.CurrentUser;

import java.security.Principal;

public record CurrentUserPrincipal(CurrentUser currentUser) implements Principal {
    public CurrentUserPrincipal {
        if (currentUser == null) {
            throw new IllegalArgumentException("currentUser must not be null");
        }
    }

    @Override
    public String getName() {
        return currentUser.userId();
    }
}
