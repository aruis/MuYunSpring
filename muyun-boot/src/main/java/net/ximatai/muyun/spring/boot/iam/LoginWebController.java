package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.common.exception.AuthenticationRequiredException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.iam.user.LoginResult;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

@ApplicationScoped
@Path("/iam.auth")
public class LoginWebController {
    private final UserSessionService userSessionService;

    public LoginWebController(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    @POST
    @Path("/login")
    public LoginResult login(LoginRequest request) {
        return userSessionService.login(request.tenantId(), request.username(), request.password());
    }

    @POST
    @Path("/logout")
    public void logout(@Context HttpHeaders headers) {
        userSessionService.logout(bearerToken(headers));
    }

    @GET
    @Path("/context")
    public CurrentUser context() {
        return CurrentUserContext.currentUser()
                .orElseThrow(() -> new AuthenticationRequiredException("current user context is not available"));
    }

    private String bearerToken(HttpHeaders headers) {
        String header = headers == null ? null : headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (header == null || header.isBlank()) {
            return null;
        }
        String prefix = "Bearer ";
        if (!header.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return null;
        }
        return header.substring(prefix.length()).trim();
    }

    public record LoginRequest(String tenantId, String username, String password) {
    }
}
