package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserProvider;
import net.ximatai.muyun.spring.iam.user.UserSessionService;

import java.util.Optional;

public class BearerTokenCurrentUserProvider implements CurrentUserProvider {
    private static final ThreadLocal<String> AUTHORIZATION_HEADER = new ThreadLocal<>();

    private final UserSessionService userSessionService;

    public BearerTokenCurrentUserProvider(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    public static Scope useAuthorizationHeader(String authorizationHeader) {
        String previous = AUTHORIZATION_HEADER.get();
        AUTHORIZATION_HEADER.set(authorizationHeader);
        return () -> {
            if (previous == null) {
                AUTHORIZATION_HEADER.remove();
            } else {
                AUTHORIZATION_HEADER.set(previous);
            }
        };
    }

    @Override
    public Optional<CurrentUser> currentUser() {
        return userSessionService.currentUser(bearerToken(AUTHORIZATION_HEADER.get()));
    }

    private String bearerToken(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        String prefix = "Bearer ";
        if (!header.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return null;
        }
        return header.substring(prefix.length()).trim();
    }

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
