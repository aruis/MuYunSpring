package net.ximatai.muyun.spring.common.platform;

public final class PlatformPermissionCode {
    private PlatformPermissionCode() {
    }

    public static String action(String moduleAlias, String actionCode) {
        return requireText(moduleAlias, "moduleAlias") + ":" + requireText(actionCode, "actionCode");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
