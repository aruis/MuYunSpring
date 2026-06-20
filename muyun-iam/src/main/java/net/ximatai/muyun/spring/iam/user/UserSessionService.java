package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.exception.AuthenticationFailedException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.model.EntityLifecycle;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.util.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final UserAccountService userAccountService;
    private final UserSessionDao userSessionDao;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public UserSessionService(UserAccountService userAccountService, UserSessionDao userSessionDao) {
        this(userAccountService, userSessionDao, Clock.systemUTC());
    }

    UserSessionService(UserAccountService userAccountService, UserSessionDao userSessionDao, Clock clock) {
        this.userAccountService = userAccountService;
        this.userSessionDao = userSessionDao;
        this.clock = clock;
    }

    public LoginResult login(String tenantId, String username, String password) {
        String validTenantId = Preconditions.requireText(tenantId, "tenantId");
        try (TenantContext.Scope ignored = TenantContext.use(validTenantId)) {
            UserAccount user = userAccountService.requireActiveUser(username);
            if (!userAccountService.passwordMatches(user, password)) {
                throw new AuthenticationFailedException("invalid username or password");
            }
            CurrentUser currentUser = CurrentUser.tenantUser(
                    user.getId(), user.getUsername(), user.getTenantId(), user.getOrganizationId());
            String token = newToken();
            Instant issuedAt = clock.instant();
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
            EntityLifecycle.prepareInsert(session, issuedAt);
            userSessionDao.insert(session);
            return LoginResult.bearer(token, issuedAt, currentUser);
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
        Instant now = clock.instant();
        if (isExpired(session, now)) {
            return Optional.empty();
        }
        try (TenantContext.Scope ignored = TenantContext.use(session.getTenantId())) {
            UserAccount user = userAccountService.select(session.getUserId());
            if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
                revoke(session, now, "user inactive");
                return Optional.empty();
            }
            if (!updateLastSeenIfDue(session, now)) {
                return Optional.empty();
            }
            CurrentUser currentUser = CurrentUser.tenantUser(
                    user.getId(), user.getUsername(), user.getTenantId(), user.getOrganizationId());
            return Optional.of(currentUser);
        }
    }

    public void logout(String token) {
        UserSession session = sessionByToken(token);
        if (session != null) {
            revoke(session, clock.instant(), "logout");
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
        Instant now = clock.instant();
        List<UserSession> sessions = userSessionDao.query(Criteria.of().eq("userId", userId), ALL);
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
        return userSessionDao.query(Criteria.of().eq("tokenHash", tokenHash(normalized)), new PageRequest(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
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
            EntityLifecycle.prepareUpdate(current, now);
            int updated = userSessionDao.updateByIdAndVersion(current, expectedVersion);
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
        EntityLifecycle.prepareUpdate(session, now);
        int updated = userSessionDao.updateByIdAndVersion(session, expectedVersion);
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

    private UserSession sessionById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return userSessionDao.query(Criteria.of().eq("id", id), new PageRequest(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
    }
}
