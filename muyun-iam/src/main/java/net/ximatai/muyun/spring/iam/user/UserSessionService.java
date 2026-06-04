package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.util.Preconditions;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserSessionService {
    private static final int TOKEN_BYTES = 32;

    private final UserAccountService userAccountService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, CurrentUser> sessions = new ConcurrentHashMap<>();

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
            sessions.put(token, currentUser);
            return LoginResult.bearer(token, Instant.now(), currentUser);
        }
    }

    public Optional<CurrentUser> currentUser(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(token.trim()));
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
}
