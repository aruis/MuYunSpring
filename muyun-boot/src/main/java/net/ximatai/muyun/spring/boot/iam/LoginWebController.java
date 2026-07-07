package net.ximatai.muyun.spring.boot.iam;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.common.exception.AuthenticationRequiredException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.iam.user.LoginResult;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/iam.auth")
public class LoginWebController {
    private final UserSessionService userSessionService;

    public LoginWebController(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    @PostMapping("/login")
    public LoginResult login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return userSessionService.login(request.tenantId(), request.username(), request.password(),
                clientIp(httpRequest), httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/changeOwnPassword")
    public void changeOwnPassword(@RequestBody ChangeOwnPasswordRequest request) {
        CurrentUser currentUser = CurrentUserContext.currentUser()
                .orElseThrow(() -> new AuthenticationRequiredException("current user context is not available"));
        userSessionService.changeOwnPassword(currentUser.userId(), request.currentPassword(), request.newPassword());
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        userSessionService.logout(bearerToken(request));
    }

    @GetMapping("/context")
    public CurrentUser context() {
        return CurrentUserContext.currentUser()
                .orElseThrow(() -> new AuthenticationRequiredException("current user context is not available"));
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

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    public record LoginRequest(String tenantId, String username, String password) {
    }

    public record ChangeOwnPasswordRequest(String currentPassword, String newPassword) {
    }
}
