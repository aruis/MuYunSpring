package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.spring.common.exception.AuthenticationFailedException;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserTimeZoneResolver;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.ability.action.BusinessExceptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.function.Supplier;

@Service
public class UserSessionService {
    private static final int TOKEN_BYTES = 32;
    private static final Duration SESSION_IDLE_TIMEOUT = Duration.ofHours(12);
    private static final Duration SESSION_ABSOLUTE_TTL = Duration.ofDays(7);
    private static final Duration LAST_SEEN_WRITE_INTERVAL = Duration.ofSeconds(60);

    private final UserAccountService userAccountService;
    private final UserSessionRecordService userSessionRecordService;
    private final UserSessionRevocationService userSessionRevocationService;
    private final ActiveTenantVerifier activeTenantVerifier;
    private final Supplier<UserSecurityEventPublisher> userSecurityEventPublisher;
    private final Supplier<UserSessionLifecycleEventPublisher> userSessionLifecycleEventPublisher;
    private final Clock clock;
    private final CurrentUserTimeZoneResolver currentUserTimeZoneResolver;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public UserSessionService(UserAccountService userAccountService,
                              UserSessionRecordService userSessionRecordService,
                              ActiveTenantVerifier activeTenantVerifier,
                              ObjectProvider<UserSecurityEventPublisher> userSecurityEventPublisher,
                              ObjectProvider<UserSessionRevocationService> userSessionRevocationService,
                              ObjectProvider<CurrentUserTimeZoneResolver> currentUserTimeZoneResolver,
                              ApplicationEventPublisher applicationEventPublisher) {
        this(userAccountService, userSessionRecordService, activeTenantVerifier,
                userSessionRevocationService == null ? null : userSessionRevocationService.getIfAvailable(),
                userSecurityEventPublisher == null
                        ? () -> UserSecurityEventPublisher.NOOP
                        : () -> userSecurityEventPublisher.getIfAvailable(() -> UserSecurityEventPublisher.NOOP),
                () -> event -> applicationEventPublisher.publishEvent(event),
                Clock.systemUTC(),
                currentUserTimeZoneResolver == null
                        ? null
                        : currentUserTimeZoneResolver.getIfAvailable(() -> CurrentUserTimeZoneResolver.NONE));
    }

    UserSessionService(UserAccountService userAccountService, UserSessionDao userSessionDao, Clock clock) {
        this(userAccountService, new UserSessionRecordService(userSessionDao), userAccountService,
                null, UserSecurityEventPublisher.NOOP, UserSessionLifecycleEventPublisher.NOOP, clock, null);
    }

    UserSessionService(UserAccountService userAccountService, UserSessionDao userSessionDao, Clock clock,
                       CurrentUserTimeZoneResolver currentUserTimeZoneResolver) {
        this(userAccountService, new UserSessionRecordService(userSessionDao), userAccountService,
                null, UserSecurityEventPublisher.NOOP, UserSessionLifecycleEventPublisher.NOOP, clock,
                currentUserTimeZoneResolver);
    }

    UserSessionService(UserAccountService userAccountService, UserSessionDao userSessionDao,
                       UserSecurityEventPublisher userSecurityEventPublisher, Clock clock) {
        this(userAccountService, new UserSessionRecordService(userSessionDao), userAccountService,
                null, userSecurityEventPublisher, UserSessionLifecycleEventPublisher.NOOP, clock, null);
    }

    UserSessionService(UserAccountService userAccountService, UserSessionDao userSessionDao,
                       UserSecurityEventPublisher userSecurityEventPublisher,
                       UserSessionLifecycleEventPublisher userSessionLifecycleEventPublisher,
                       Clock clock) {
        this(userAccountService, new UserSessionRecordService(userSessionDao), userAccountService,
                null, userSecurityEventPublisher, userSessionLifecycleEventPublisher, clock, null);
    }

    UserSessionService(UserAccountService userAccountService,
                       UserSessionRecordService userSessionRecordService,
                       Clock clock) {
        this(userAccountService, userSessionRecordService, userAccountService,
                null, UserSecurityEventPublisher.NOOP, UserSessionLifecycleEventPublisher.NOOP, clock, null);
    }

    UserSessionService(UserAccountService userAccountService,
                       UserSessionRecordService userSessionRecordService,
                       ActiveTenantVerifier activeTenantVerifier,
                       Clock clock) {
        this(userAccountService, userSessionRecordService, activeTenantVerifier,
                null, UserSecurityEventPublisher.NOOP, UserSessionLifecycleEventPublisher.NOOP, clock, null);
    }

    UserSessionService(UserAccountService userAccountService,
                       UserSessionRecordService userSessionRecordService,
                       ActiveTenantVerifier activeTenantVerifier,
                       UserSessionRevocationService userSessionRevocationService,
                       UserSecurityEventPublisher userSecurityEventPublisher,
                       UserSessionLifecycleEventPublisher userSessionLifecycleEventPublisher,
                       Clock clock) {
        this(userAccountService, userSessionRecordService, activeTenantVerifier, userSessionRevocationService,
                userSecurityEventPublisher, userSessionLifecycleEventPublisher, clock, null);
    }

    UserSessionService(UserAccountService userAccountService,
                       UserSessionRecordService userSessionRecordService,
                       ActiveTenantVerifier activeTenantVerifier,
                       UserSessionRevocationService userSessionRevocationService,
                       UserSecurityEventPublisher userSecurityEventPublisher,
                       UserSessionLifecycleEventPublisher userSessionLifecycleEventPublisher,
                       Clock clock,
                       CurrentUserTimeZoneResolver currentUserTimeZoneResolver) {
        this(userAccountService, userSessionRecordService, activeTenantVerifier,
                userSessionRevocationService,
                () -> userSecurityEventPublisher == null ? UserSecurityEventPublisher.NOOP : userSecurityEventPublisher,
                () -> userSessionLifecycleEventPublisher == null
                        ? UserSessionLifecycleEventPublisher.NOOP
                        : userSessionLifecycleEventPublisher,
                clock,
                currentUserTimeZoneResolver);
    }

    UserSessionService(UserAccountService userAccountService,
                       UserSessionRecordService userSessionRecordService,
                       ActiveTenantVerifier activeTenantVerifier,
                       UserSessionRevocationService userSessionRevocationService,
                       Supplier<UserSecurityEventPublisher> userSecurityEventPublisher,
                       Supplier<UserSessionLifecycleEventPublisher> userSessionLifecycleEventPublisher,
                       Clock clock,
                       CurrentUserTimeZoneResolver currentUserTimeZoneResolver) {
        this.userAccountService = userAccountService;
        this.userSessionRecordService = userSessionRecordService;
        this.userSessionRevocationService = userSessionRevocationService == null
                ? new UserSessionRevocationService(userSessionRecordService, userSessionLifecycleEventPublisher, clock)
                : userSessionRevocationService;
        this.activeTenantVerifier = activeTenantVerifier;
        this.userSecurityEventPublisher = userSecurityEventPublisher == null
                ? () -> UserSecurityEventPublisher.NOOP
                : userSecurityEventPublisher;
        this.userSessionLifecycleEventPublisher = userSessionLifecycleEventPublisher == null
                ? () -> UserSessionLifecycleEventPublisher.NOOP
                : userSessionLifecycleEventPublisher;
        this.clock = clock;
        this.currentUserTimeZoneResolver = currentUserTimeZoneResolver == null
                ? CurrentUserTimeZoneResolver.NONE
                : currentUserTimeZoneResolver;
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
            session.setLoginIp(normalizeBlank(ip));
            session.setLoginUserAgent(normalizeBlank(userAgent));
            String sessionId = userSessionRecordService.issue(session);
            if (session.getId() == null || session.getId().isBlank()) {
                session.setId(sessionId);
            }
            userAccountService.recordLoginSuccess(user.getId(), issuedAt, ip, userAgent);
            userSessionLifecycleEventPublisher.get()
                    .publish(UserSessionLifecycleEvent.loggedIn(currentUser.userId(), session.getId()));
            return LoginResult.bearer(token, session.getId(), issuedAt, currentUser,
                    passwordChangeRequired,
                    userAccountService.effectivePasswordStatus(user),
                    user.getPasswordExpiresAt());
        }
    }

    public Optional<CurrentUser> currentUser(String token) {
        return currentUser(token, true);
    }

    public Optional<CurrentUser> currentUserSnapshot(String token) {
        return currentUser(token, false);
    }

    private Optional<CurrentUser> currentUser(String token, boolean touchSession) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        UserSession session = sessionByToken(token);
        if (session == null || session.getRevokedAt() != null) {
            return Optional.empty();
        }
        Instant now = now();
        if (isExpired(session, now)) {
            if (touchSession) {
                userSessionRevocationService.revoke(session, now, "session expired");
            }
            return Optional.empty();
        }
        try (TenantContext.Scope ignored = sessionTenantScope(session.getTenantId())) {
            if (!verifyActiveTenantForSession(session, now, touchSession)) {
                return Optional.empty();
            }
            UserAccount user = userAccountService.select(session.getUserId());
            if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
                if (touchSession) {
                    userSessionRevocationService.revoke(session, now, "user inactive");
                }
                return Optional.empty();
            }
            if (touchSession && !updateLastSeenIfDue(session, now)) {
                return Optional.empty();
            }
            return Optional.of(currentUserOf(user, Boolean.TRUE.equals(session.getPasswordChangeRequired())));
        }
    }

    public void logout(String token) {
        UserSession session = sessionByToken(token);
        if (session != null) {
            userSessionRevocationService.logout(session, now());
        }
    }

    public int changeOwnPassword(String userId, String currentPassword, String newPassword) {
        return userAccountService.changeOwnPassword(userId, currentPassword, newPassword);
    }

    private void verifyActiveTenantForLogin(String tenantId) {
        try {
            activeTenantVerifier.verifyActiveTenant(tenantId);
        } catch (PlatformException exception) {
            throw new AuthenticationFailedException("invalid username or password", exception);
        }
    }

    private boolean verifyActiveTenantForSession(UserSession session, Instant now, boolean revokeWhenInactive) {
        if (session.getTenantId() == null || session.getTenantId().isBlank()) {
            return true;
        }
        try {
            activeTenantVerifier.verifyActiveTenant(session.getTenantId());
            return true;
        } catch (PlatformException exception) {
            if (revokeWhenInactive) {
                userSessionRevocationService.revoke(session, now, "tenant inactive");
            }
            return false;
        }
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public int revokeUserSessions(String userId) {
        return userSessionRevocationService.revokeUserSessions(userId, "user sessions revoked");
    }

    public List<UserSessionView> activeSessionsOfUser(String userId, String currentToken) {
        String validUserId = normalizeBlank(userId);
        if (validUserId == null) {
            return List.of();
        }
        Instant now = now();
        String currentTokenHash = currentTokenHash(currentToken);
        return userSessionRecordService.listByUserId(validUserId).stream()
                .filter(session -> isActive(session, now))
                .map(session -> UserSessionView.from(session, currentTokenHash != null
                        && currentTokenHash.equals(session.getTokenHash())))
                .toList();
    }

    public List<UserSessionStatusView> activeSessionStatuses(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        Instant now = now();
        return userIds.stream()
                .map(this::normalizeBlank)
                .filter(userId -> userId != null)
                .distinct()
                .map(userId -> {
                    long count = userSessionRecordService.listByUserId(userId).stream()
                            .filter(session -> isActive(session, now))
                            .count();
                    return new UserSessionStatusView(userId, count > 0, count);
                })
                .toList();
    }

    public UserSessionStatusView activeSessionStatus(String userId) {
        String validUserId = normalizeBlank(userId);
        if (validUserId == null) {
            return new UserSessionStatusView(null, false, 0);
        }
        return activeSessionStatuses(List.of(validUserId)).stream()
                .findFirst()
                .orElse(new UserSessionStatusView(validUserId, false, 0));
    }

    public int revokeUserSession(String userId, String sessionId, String currentToken) {
        String validUserId = normalizeBlank(userId);
        String validSessionId = normalizeBlank(sessionId);
        if (validUserId == null || validSessionId == null) {
            return 0;
        }
        UserSession session = userSessionRecordService.findById(validSessionId);
        if (session == null || !validUserId.equals(session.getUserId())) {
            return 0;
        }
        rejectCurrentSessionRevoke(session, currentToken);
        Instant now = now();
        if (!isActive(session, now)) {
            return 0;
        }
        if (!userSessionRevocationService.revoke(session, now, "user session revoked by administrator")) {
            return 0;
        }
        userSecurityEventPublisher.get().publish(UserSecurityEvent.sessionRevoked(validUserId, validSessionId));
        return 1;
    }

    public int revokeUserSessions(String userId, List<String> sessionIds, String currentToken) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String sessionId : sessionIds) {
            count += revokeUserSession(userId, sessionId, currentToken);
        }
        return count;
    }

    private UserSession sessionByToken(String token) {
        String normalized = normalizeToken(token);
        if (normalized == null) {
            return null;
        }
        return userSessionRecordService.findByTokenHash(tokenHash(normalized));
    }

    private String currentTokenHash(String token) {
        String normalized = normalizeToken(token);
        return normalized == null ? null : tokenHash(normalized);
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

    private boolean isActive(UserSession session, Instant now) {
        return session != null && session.getRevokedAt() == null && !isExpired(session, now);
    }

    private void rejectCurrentSessionRevoke(UserSession session, String currentToken) {
        String currentTokenHash = currentTokenHash(currentToken);
        if (currentTokenHash != null && currentTokenHash.equals(session.getTokenHash())) {
            throw BusinessExceptions.warning("iam.user-session.revoke-current-denied",
                    "cannot revoke current session from user management");
        }
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
        CurrentUser currentUser;
        if (user.getTenantId() == null || user.getTenantId().isBlank()) {
            currentUser = CurrentUser.systemUser(user.getId(), user.getUsername(), passwordChangeRequired);
        } else {
            currentUser = CurrentUser.tenantUser(user.getId(), user.getUsername(), user.getTenantId(),
                    user.getOrganizationId(), passwordChangeRequired);
        }
        return currentUserTimeZoneResolver.resolveZoneId(currentUser)
                .map(zoneId -> currentUser.withTimeZone(zoneId.getId()))
                .orElse(currentUser);
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
