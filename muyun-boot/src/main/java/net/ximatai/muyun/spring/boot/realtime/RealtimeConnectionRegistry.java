package net.ximatai.muyun.spring.boot.realtime;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RealtimeConnectionRegistry {
    private final ConcurrentMap<String, CurrentUserPrincipal> principalsBySessionId = new ConcurrentHashMap<>();

    public void register(String sessionId, CurrentUserPrincipal principal) {
        if (sessionId == null || sessionId.isBlank() || principal == null) {
            return;
        }
        principalsBySessionId.put(sessionId, principal);
    }

    public void unregister(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        principalsBySessionId.remove(sessionId);
    }

    public List<CurrentUserPrincipal> principals() {
        return List.copyOf(principalsBySessionId.values());
    }

    boolean contains(String sessionId, CurrentUserPrincipal principal) {
        return Objects.equals(principalsBySessionId.get(sessionId), principal);
    }
}
