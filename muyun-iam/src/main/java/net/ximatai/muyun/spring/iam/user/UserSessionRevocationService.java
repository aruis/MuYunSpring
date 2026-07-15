package net.ximatai.muyun.spring.iam.user;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;

@Service
public class UserSessionRevocationService {
    private final UserSessionRecordService userSessionRecordService;
    private final Supplier<UserSessionLifecycleEventPublisher> userSessionLifecycleEventPublisher;
    private final Clock clock;

    public UserSessionRevocationService(
            UserSessionRecordService userSessionRecordService,
            ObjectProvider<UserSessionLifecycleEventPublisher> userSessionLifecycleEventPublisher) {
        this(userSessionRecordService,
                userSessionLifecycleEventPublisher == null
                        ? () -> UserSessionLifecycleEventPublisher.NOOP
                        : () -> userSessionLifecycleEventPublisher.getIfAvailable(
                        () -> UserSessionLifecycleEventPublisher.NOOP),
                Clock.systemUTC());
    }

    UserSessionRevocationService(UserSessionRecordService userSessionRecordService,
                                 UserSessionLifecycleEventPublisher userSessionLifecycleEventPublisher,
                                 Clock clock) {
        this(userSessionRecordService,
                () -> userSessionLifecycleEventPublisher == null
                        ? UserSessionLifecycleEventPublisher.NOOP
                        : userSessionLifecycleEventPublisher,
                clock);
    }

    UserSessionRevocationService(UserSessionRecordService userSessionRecordService,
                                 Supplier<UserSessionLifecycleEventPublisher> userSessionLifecycleEventPublisher,
                                 Clock clock) {
        this.userSessionRecordService = userSessionRecordService;
        this.userSessionLifecycleEventPublisher = userSessionLifecycleEventPublisher == null
                ? () -> UserSessionLifecycleEventPublisher.NOOP
                : userSessionLifecycleEventPublisher;
        this.clock = clock;
    }

    public int revokeUserSessions(String userId, String reason) {
        String validUserId = normalizeBlank(userId);
        if (validUserId == null) {
            return 0;
        }
        Instant now = now();
        List<UserSession> sessions = userSessionRecordService.listByUserId(validUserId);
        int count = 0;
        for (UserSession session : sessions) {
            if (session.getRevokedAt() == null && revoke(session, now, reason)) {
                count += 1;
            }
        }
        return count;
    }

    public boolean revoke(UserSession session, Instant now, String reason) {
        UserSession current = session;
        for (int attempt = 0; attempt < 2 && current != null && current.getRevokedAt() == null; attempt++) {
            Integer expectedVersion = current.getVersion();
            current.setRevokedAt(now);
            current.setRevokedReason(reason);
            int updated = userSessionRecordService.updateSession(current, expectedVersion, now);
            if (updated > 0) {
                publishSessionRevoked(current);
                return true;
            }
            current = sessionById(current.getId());
        }
        return false;
    }

    private void publishSessionRevoked(UserSession session) {
        String userId = normalizeBlank(session.getUserId());
        String sessionId = normalizeBlank(session.getId());
        if (userId == null || sessionId == null) {
            return;
        }
        userSessionLifecycleEventPublisher.get().publish(UserSessionLifecycleEvent.revoked(userId, sessionId));
    }

    private UserSession sessionById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return userSessionRecordService.findById(id);
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
