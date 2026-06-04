package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.util.Preconditions;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserSessionService {
    private static final int TOKEN_BYTES = 32;
    private static final Duration SESSION_TTL = Duration.ofHours(12);

    private final UserAccountService userAccountService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    public UserSessionService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    public LoginResult login(String tenantId, String username, String password) {
        String validTenantId = Preconditions.requireText(tenantId, "tenantId");
        try (TenantContext.Scope ignored = TenantContext.use(validTenantId)) {
            UserAccount user = userAccountService.requireActiveUser(username);
            if (!userAccountService.passwordMatches(user, password)) {
                throw new PlatformException("invalid username or password");
            }
            CurrentUser currentUser = CurrentUser.tenantUser(
                    user.getId(), user.getUsername(), user.getTenantId(), user.getOrganizationId());
            String token = newToken();
            Instant issuedAt = Instant.now();
            sessions.put(token, new SessionState(currentUser, issuedAt.plus(SESSION_TTL)));
            return LoginResult.bearer(token, issuedAt, currentUser);
        }
    }

    public Optional<CurrentUser> currentUser(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String normalized = token.trim();
        SessionState state = sessions.get(normalized);
        if (state == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(state.expiresAt())) {
            sessions.remove(normalized);
            return Optional.empty();
        }
        CurrentUser currentUser = state.currentUser();
        try (TenantContext.Scope ignored = TenantContext.use(currentUser.tenantId())) {
            UserAccount user = userAccountService.select(currentUser.userId());
            if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
                sessions.remove(normalized);
                return Optional.empty();
            }
            return Optional.of(CurrentUser.tenantUser(
                    user.getId(), user.getUsername(), user.getTenantId(), user.getOrganizationId()));
        }
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            sessions.remove(token.trim());
        }
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public void revokeUserSessions(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        sessions.entrySet().removeIf(entry -> userId.equals(entry.getValue().currentUser().userId()));
    }

    private record SessionState(CurrentUser currentUser, Instant expiresAt) {
    }
}
