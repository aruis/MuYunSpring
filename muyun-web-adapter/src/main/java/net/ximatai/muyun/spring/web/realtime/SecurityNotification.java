package net.ximatai.muyun.spring.web.realtime;

public record SecurityNotification(
        String code,
        String message,
        boolean logoutRequired,
        String targetSessionId
) {
    public static final String PASSWORD_CHANGED = "platform.security.password-changed";
    public static final String PASSWORD_RESET = "platform.security.password-reset";
    public static final String FORCE_LOGOUT = "platform.security.force-logout";
    public static final String SESSION_REVOKED = "platform.security.session-revoked";

    public SecurityNotification(String code, String message, boolean logoutRequired) {
        this(code, message, logoutRequired, null);
    }

    public SecurityNotification {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("security notification code must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("security notification message must not be blank");
        }
        code = code.trim();
        message = message.trim();
        targetSessionId = targetSessionId == null ? null : targetSessionId.trim();
    }

    public static SecurityNotification passwordChanged() {
        return new SecurityNotification(PASSWORD_CHANGED, "你的密码已修改，请重新登录", true);
    }

    public static SecurityNotification passwordReset() {
        return new SecurityNotification(PASSWORD_RESET, "你的密码已被重置，请重新登录", true);
    }

    public static SecurityNotification forceLogout() {
        return new SecurityNotification(FORCE_LOGOUT, "你的登录会话已被管理员强制下线，请重新登录", true);
    }

    public static SecurityNotification sessionRevoked(String sessionId) {
        return new SecurityNotification(SESSION_REVOKED, "当前登录会话已被管理员下线，请重新登录", true, sessionId);
    }
}
