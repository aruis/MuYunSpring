package net.ximatai.muyun.spring.boot.web;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserProvider;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

public class BearerTokenCurrentUserProvider implements CurrentUserProvider {
    private final UserSessionService userSessionService;

    public BearerTokenCurrentUserProvider(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    @Override
    public Optional<CurrentUser> currentUser() {
        return request().flatMap(request -> userSessionService.currentUser(bearerToken(request)));
    }

    private Optional<HttpServletRequest> request() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return Optional.of(servletAttributes.getRequest());
        }
        return Optional.empty();
    }

    private String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            return null;
        }
        String prefix = "Bearer ";
        if (!header.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return null;
        }
        return header.substring(prefix.length()).trim();
    }
}
