package net.ximatai.muyun.spring.iam.user;

public record UserSessionLifecycleEvent(
        Type type,
        String userId,
        String sessionId
) {
    public UserSessionLifecycleEvent {
        if (type == null) {
            throw new IllegalArgumentException("user session lifecycle event type must not be null");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user session lifecycle event userId must not be blank");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("user session lifecycle event sessionId must not be blank");
        }
        userId = userId.trim();
        sessionId = sessionId.trim();
    }

    public static UserSessionLifecycleEvent loggedIn(String userId, String sessionId) {
        return new UserSessionLifecycleEvent(Type.LOGGED_IN, userId, sessionId);
    }

    public static UserSessionLifecycleEvent revoked(String userId, String sessionId) {
        return new UserSessionLifecycleEvent(Type.REVOKED, userId, sessionId);
    }

    public static UserSessionLifecycleEvent loggedOut(String userId, String sessionId) {
        return new UserSessionLifecycleEvent(Type.LOGGED_OUT, userId, sessionId);
    }

    public static UserSessionLifecycleEvent presenceConnected(String userId, String sessionId) {
        return new UserSessionLifecycleEvent(Type.PRESENCE_CONNECTED, userId, sessionId);
    }

    public static UserSessionLifecycleEvent presenceDisconnected(String userId, String sessionId) {
        return new UserSessionLifecycleEvent(Type.PRESENCE_DISCONNECTED, userId, sessionId);
    }

    public static UserSessionLifecycleEvent presenceIdle(String userId, String sessionId) {
        return new UserSessionLifecycleEvent(Type.PRESENCE_IDLE, userId, sessionId);
    }

    public static UserSessionLifecycleEvent presenceActive(String userId, String sessionId) {
        return new UserSessionLifecycleEvent(Type.PRESENCE_ACTIVE, userId, sessionId);
    }

    public enum Type {
        LOGGED_IN,
        LOGGED_OUT,
        REVOKED,
        PRESENCE_CONNECTED,
        PRESENCE_DISCONNECTED,
        PRESENCE_IDLE,
        PRESENCE_ACTIVE
    }
}
