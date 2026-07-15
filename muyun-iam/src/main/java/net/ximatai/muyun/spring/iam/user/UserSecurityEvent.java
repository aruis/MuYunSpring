package net.ximatai.muyun.spring.iam.user;

public record UserSecurityEvent(
        Type type,
        String userId,
        String sessionId
) {
    public UserSecurityEvent(Type type, String userId) {
        this(type, userId, null);
    }

    public UserSecurityEvent {
        if (type == null) {
            throw new IllegalArgumentException("user security event type must not be null");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user security event userId must not be blank");
        }
        if (type == Type.SESSION_REVOKED && (sessionId == null || sessionId.isBlank())) {
            throw new IllegalArgumentException("session revoked event sessionId must not be blank");
        }
        userId = userId.trim();
        sessionId = sessionId == null ? null : sessionId.trim();
    }

    public static UserSecurityEvent passwordChanged(String userId) {
        return new UserSecurityEvent(Type.PASSWORD_CHANGED, userId);
    }

    public static UserSecurityEvent passwordReset(String userId) {
        return new UserSecurityEvent(Type.PASSWORD_RESET, userId);
    }

    public static UserSecurityEvent forceLogout(String userId) {
        return new UserSecurityEvent(Type.FORCE_LOGOUT, userId);
    }

    public static UserSecurityEvent sessionRevoked(String userId, String sessionId) {
        return new UserSecurityEvent(Type.SESSION_REVOKED, userId, sessionId);
    }

    public enum Type {
        PASSWORD_CHANGED,
        PASSWORD_RESET,
        FORCE_LOGOUT,
        SESSION_REVOKED
    }
}
