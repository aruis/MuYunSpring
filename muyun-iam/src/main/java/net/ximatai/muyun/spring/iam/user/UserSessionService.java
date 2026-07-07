package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.spring.common.exception.AuthenticationFailedException;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class UserSessionService {
    private static final int TOKEN_BYTES = 32;
    private static final Duration SESSION_IDLE_TIMEOUT = Duration.ofHours(12);
    private static final Duration SESSION_ABSOLUTE_TTL = Duration.ofDays(7);
    private static final Duration LAST_SEEN_WRITE_INTERVAL = Duration.ofSeconds(60);

    private final UserAccountService userAccountService;
    private final UserSessionRecordService userSessionRecordService;
    private final ActiveTenantVerifier activeTenantVerifier;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public UserSessionService(UserAccountService userAccountService,
                              UserSessionRecordService userSessionRecordService,
                              ActiveTenantVerifier activeTenantVerifier) {
        this(userAccountService, userSessionRecordService, activeTenantVerifier, Clock.systemUTC());
    }

    UserSessionService(UserAccountService userAccountService, UserSessionDao userSessionDao, Clock clock) {
        this(userAccountService, new UserSessionRecordService(userSessionDao), userAccountService, clock);
    }

    UserSessionService(UserAccountService userAccountService,
                       UserSessionRecordService userSessionRecordService,
                       Clock clock) {
        this(userAccountService, userSessionRecordService, userAccountService, clock);
    }

    UserSessionService(UserAccountService userAccountService,
                       UserSessionRecordService userSessionRecordService,
                       ActiveTenantVerifier activeTenantVerifier,
                       Clock clock) {
        this.userAccountService = userAccountService;
        this.userSessionRecordService = userSessionRecordService;
        this.activeTenantVerifier = activeTenantVerifier;
        this.clock = clock;
    }

    public LoginResult login(String tenantId, String username, String password) {
        return login(tenantId, username, password, null, null);
    }

    public LoginResult login(String tenantId, String username, String password, String ip, String userAgent) {
        String normalizedTenantId = normalizeBlank(tenantId);
        try (TenantContext.Scope ignored = loginTenantScope(normalizedTenantId)) {
            if (normalizedTenantId != null) {
                verifyActiveTenantForLogin(normalizedTenantId);
            }
            UserAccount user = userAccountService.requireActiveUser(normalizedTenantId, username);
            if (!userAccountService.passwordMatches(user, password)) {
                userAccountService.recordLoginFailure(user, now());
                throw new AuthenticationFailedException("invalid username or password");
            }
            String token = newToken();
            Instant issuedAt = now();
            if (userAccountService.resetPasswordExpired(user, issuedAt)) {
                userAccountService.recordLoginFailure(user, issuedAt);
                throw new AuthenticationFailedException("temporary password expired");
            }
            boolean passwordChangeRequired = userAccountService.passwordChangeRequired(user, issuedAt);
            CurrentUser currentUser = currentUserOf(user, passwordChangeRequired);
            Instant maxExpiresAt = issuedAt.plus(SESSION_ABSOLUTE_TTL);
            UserSession session = new UserSession();
            session.setTenantId(currentUser.tenantId());
            session.setUserId(currentUser.userId());
            session.setUsername(currentUser.username());
            session.setOrganizationId(currentUser.organizationId());
            session.setTokenHash(tokenHash(token));
            session.setIssuedAt(issuedAt);
            session.setExpiresAt(nextIdleExpiresAt(issuedAt, maxExpiresAt));
            session.setMaxExpiresAt(maxExpiresAt);
            session.setLastSeenAt(issuedAt);
            session.setPasswordChangeRequired(passwordChangeRequired);
            userSessionRecordService.issue(session);
            userAccountService.recordLoginSuccess(user.getId(), issuedAt, ip, userAgent);
            return LoginResult.bearer(token, issuedAt, currentUser,
                    passwordChangeRequired,
                    userAccountService.effectivePasswordStatus(user),
                    user.getPasswordExpiresAt());
        }
    }

    public Optional<CurrentUser> currentUser(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        UserSession session = sessionByToken(token);
        if (session == null || session.getRevokedAt() != null) {
            return Optional.empty();
        }
        Instant now = now();
        if (isExpired(session, now)) {
            return Optional.empty();
        }
        try (TenantContext.Scope ignored = sessionTenantScope(session.getTenantId())) {
            if (!verifyActiveTenantForSession(session, now)) {
                return Optional.empty();
            }
            UserAccount user = userAccountService.select(session.getUserId());
            if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
                revoke(session, now, "user inactive");
                return Optional.empty();
            }
            if (!updateLastSeenIfDue(session, now)) {
                return Optional.empty();
            }
            return Optional.of(currentUserOf(user, Boolean.TRUE.equals(session.getPasswordChangeRequired())));
        }
    }

    public void logout(String token) {
        UserSession session = sessionByToken(token);
        if (session != null) {
            revoke(session, now(), "logout");
        }
    }

    public int changeOwnPassword(String userId, String currentPassword, String newPassword) {
        int changed = userAccountService.changeOwnPassword(userId, currentPassword, newPassword);
        if (changed > 0) {
            revokeUserSessions(userId);
        }
        return changed;
    }

    private void verifyActiveTenantForLogin(String tenantId) {
        try {
            activeTenantVerifier.verifyActiveTenant(tenantId);
        } catch (PlatformException exception) {
            throw new AuthenticationFailedException("invalid username or password", exception);
        }
    }

    private boolean verifyActiveTenantForSession(UserSession session, Instant now) {
        if (session.getTenantId() == null || session.getTenantId().isBlank()) {
            return true;
        }
        try {
            activeTenantVerifier.verifyActiveTenant(session.getTenantId());
            return true;
        } catch (PlatformException exception) {
            revoke(session, now, "tenant inactive");
            return false;
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
        Instant now = now();
        List<UserSession> sessions = userSessionRecordService.listByUserId(userId);
        for (UserSession session : sessions) {
            if (session.getRevokedAt() == null) {
                revoke(session, now, "user sessions revoked");
            }
        }
    }

    private UserSession sessionByToken(String token) {
        String normalized = normalizeToken(token);
        if (normalized == null) {
            return null;
        }
        return userSessionRecordService.findByTokenHash(tokenHash(normalized));
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return token.trim();
    }

    private String tokenHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private void revoke(UserSession session, Instant now, String reason) {
        UserSession current = session;
        for (int attempt = 0; attempt < 2 && current != null && current.getRevokedAt() == null; attempt++) {
            Integer expectedVersion = current.getVersion();
            current.setRevokedAt(now);
            current.setRevokedReason(reason);
            int updated = userSessionRecordService.updateSession(current, expectedVersion, now);
            if (updated > 0) {
                return;
            }
            current = sessionById(current.getId());
        }
    }

    private boolean updateLastSeenIfDue(UserSession session, Instant now) {
        Instant lastSeenAt = session.getLastSeenAt();
        if (lastSeenAt != null && lastSeenAt.plus(LAST_SEEN_WRITE_INTERVAL).isAfter(now)) {
            return true;
        }
        Integer expectedVersion = session.getVersion();
        session.setLastSeenAt(now);
        session.setMaxExpiresAt(effectiveMaxExpiresAt(session));
        session.setExpiresAt(nextIdleExpiresAt(now, session.getMaxExpiresAt()));
        int updated = userSessionRecordService.updateSession(session, expectedVersion, now);
        if (updated > 0) {
            return true;
        }
        UserSession latest = sessionById(session.getId());
        return latest != null && latest.getRevokedAt() == null && !isExpired(latest, now);
    }

    private boolean isExpired(UserSession session, Instant now) {
        return !now.isBefore(session.getExpiresAt()) || !now.isBefore(effectiveMaxExpiresAt(session));
    }

    private Instant effectiveMaxExpiresAt(UserSession session) {
        if (session.getMaxExpiresAt() != null) {
            return session.getMaxExpiresAt();
        }
        if (session.getIssuedAt() != null) {
            return session.getIssuedAt().plus(SESSION_ABSOLUTE_TTL);
        }
        return session.getExpiresAt();
    }

    private Instant nextIdleExpiresAt(Instant now, Instant maxExpiresAt) {
        Instant idleExpiresAt = now.plus(SESSION_IDLE_TIMEOUT);
        return idleExpiresAt.isBefore(maxExpiresAt) ? idleExpiresAt : maxExpiresAt;
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private TenantContext.Scope loginTenantScope(String tenantId) {
        return tenantId == null
                ? TenantContext.system("system user login")
                : TenantContext.use(tenantId);
    }

    private TenantContext.Scope sessionTenantScope(String tenantId) {
        return tenantId == null || tenantId.isBlank()
                ? TenantContext.system("system user session")
                : TenantContext.use(tenantId);
    }

    private CurrentUser currentUserOf(UserAccount user, boolean passwordChangeRequired) {
        if (user.getTenantId() == null || user.getTenantId().isBlank()) {
            return CurrentUser.systemUser(user.getId(), user.getUsername(), passwordChangeRequired);
        }
        return CurrentUser.tenantUser(user.getId(), user.getUsername(), user.getTenantId(),
                user.getOrganizationId(), passwordChangeRequired);
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private UserSession sessionById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return userSessionRecordService.findById(id);
    }
}
