package net.ximatai.muyun.spring.iam.user;

import java.time.Duration;
import java.time.Instant;

public record UserSessionPresence(
        String sessionId,
        boolean present,
        long connectionCount,
        Instant lastConnectedAt,
        Instant lastObservedAt
) {
    public static final Duration IDLE_TIMEOUT = Duration.ofMinutes(3);

    public UserSessionPresence {
        sessionId = sessionId == null || sessionId.isBlank() ? null : sessionId.trim();
        if (connectionCount < 0) {
            connectionCount = 0;
        }
    }

    public static UserSessionPresence absent(String sessionId) {
        return new UserSessionPresence(sessionId, false, 0, null, null);
    }

    public boolean idleSince(Instant now) {
        return idleSince(null, now);
    }

    public boolean idleSince(Instant lastSeenAt, Instant now) {
        Instant lastActivityAt = lastObservedAt == null ? lastSeenAt : lastObservedAt;
        return present && lastActivityAt != null && now != null
                && !lastActivityAt.plus(IDLE_TIMEOUT).isAfter(now);
    }
}
