package net.ximatai.muyun.spring.iam.web.realtime;

import net.ximatai.muyun.spring.iam.user.UserSessionPresence;
import net.ximatai.muyun.spring.iam.user.UserSessionPresenceLookup;
import net.ximatai.muyun.spring.web.realtime.CurrentUserPrincipal;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RealtimeConnectionRegistry implements UserSessionPresenceLookup {
    private final ConcurrentMap<String, ConnectionEntry> connectionsByWebSocketSessionId = new ConcurrentHashMap<>();
    private final Clock clock;

    public RealtimeConnectionRegistry() {
        this(Clock.systemUTC());
    }

    public RealtimeConnectionRegistry(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public boolean register(String webSocketSessionId, CurrentUserPrincipal principal) {
        if (webSocketSessionId == null || webSocketSessionId.isBlank() || principal == null) {
            return false;
        }
        Instant now = clock.instant();
        boolean[] changed = {false};
        connectionsByWebSocketSessionId.compute(webSocketSessionId, (key, previous) -> {
            changed[0] = previous == null || !Objects.equals(previous.principal(), principal);
            Instant connectedAt = previous == null ? now : previous.connectedAt();
            return new ConnectionEntry(principal, connectedAt, now);
        });
        return changed[0];
    }

    public CurrentUserPrincipal unregister(String webSocketSessionId) {
        if (webSocketSessionId == null || webSocketSessionId.isBlank()) {
            return null;
        }
        ConnectionEntry removed = connectionsByWebSocketSessionId.remove(webSocketSessionId);
        return removed == null ? null : removed.principal();
    }

    @Override
    public UserSessionPresence presenceOf(String sessionId) {
        String normalizedSessionId = normalize(sessionId);
        if (normalizedSessionId == null) {
            return UserSessionPresence.absent(sessionId);
        }
        long count = 0;
        Instant lastConnectedAt = null;
        Instant lastObservedAt = null;
        for (ConnectionEntry entry : connectionsByWebSocketSessionId.values()) {
            if (!normalizedSessionId.equals(entry.principal().loginSessionId())) {
                continue;
            }
            count++;
            lastConnectedAt = latest(lastConnectedAt, entry.connectedAt());
            lastObservedAt = latest(lastObservedAt, entry.lastObservedAt());
        }
        return new UserSessionPresence(normalizedSessionId, count > 0, count, lastConnectedAt, lastObservedAt);
    }

    public void touch(String webSocketSessionId) {
        if (webSocketSessionId == null || webSocketSessionId.isBlank()) {
            return;
        }
        connectionsByWebSocketSessionId.computeIfPresent(webSocketSessionId,
                (key, entry) -> new ConnectionEntry(entry.principal(), entry.connectedAt(), clock.instant()));
    }

    public List<CurrentUserPrincipal> principals() {
        return connectionsByWebSocketSessionId.values().stream()
                .map(ConnectionEntry::principal)
                .toList();
    }

    public List<SessionPresenceEntry> sessionPresenceEntries() {
        Map<String, SessionPresenceEntry> entries = new HashMap<>();
        for (ConnectionEntry entry : connectionsByWebSocketSessionId.values()) {
            String sessionId = entry.principal().loginSessionId();
            if (sessionId == null) {
                continue;
            }
            entries.merge(sessionId, new SessionPresenceEntry(entry.principal().currentUser().userId(),
                            sessionId, entry.lastObservedAt()),
                    (left, right) -> latest(left.lastObservedAt(), right.lastObservedAt()) == left.lastObservedAt()
                            ? left
                            : right);
        }
        return List.copyOf(entries.values());
    }

    boolean contains(String webSocketSessionId, CurrentUserPrincipal principal) {
        ConnectionEntry entry = connectionsByWebSocketSessionId.get(webSocketSessionId);
        return entry != null && Objects.equals(entry.principal(), principal);
    }

    private static Instant latest(Instant left, Instant right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return right.isAfter(left) ? right : left;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ConnectionEntry(
            CurrentUserPrincipal principal,
            Instant connectedAt,
            Instant lastObservedAt
    ) {
    }

    public record SessionPresenceEntry(
            String userId,
            String sessionId,
            Instant lastObservedAt
    ) {
    }
}
