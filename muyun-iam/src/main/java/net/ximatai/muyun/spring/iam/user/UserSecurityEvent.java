package net.ximatai.muyun.spring.iam.user;

public record UserSecurityEvent(
        Type type,
        String userId
) {
    public UserSecurityEvent {
        if (type == null) {
            throw new IllegalArgumentException("user security event type must not be null");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user security event userId must not be blank");
        }
        userId = userId.trim();
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

    public enum Type {
        PASSWORD_CHANGED,
        PASSWORD_RESET,
        FORCE_LOGOUT
    }
}
