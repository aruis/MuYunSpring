package net.ximatai.muyun.spring.boot.realtime;

public record SecurityNotification(
        String code,
        String message,
        boolean logoutRequired
) {
    public static final String PASSWORD_CHANGED = "platform.security.password-changed";
    public static final String PASSWORD_RESET = "platform.security.password-reset";

    public SecurityNotification {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("security notification code must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("security notification message must not be blank");
        }
        code = code.trim();
        message = message.trim();
    }

    public static SecurityNotification passwordChanged() {
        return new SecurityNotification(PASSWORD_CHANGED, "你的密码已修改，请重新登录", true);
    }

    public static SecurityNotification passwordReset() {
        return new SecurityNotification(PASSWORD_RESET, "你的密码已被重置，请重新登录", true);
    }
}
