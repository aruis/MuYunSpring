package net.ximatai.muyun.spring.boot.web;

public record PlatformWebError(String code, int status, String message) {
    public static PlatformWebError authenticationRequired(String message) {
        return new PlatformWebError("AUTHENTICATION_REQUIRED", 401, message);
    }

    public static PlatformWebError authenticationFailed(String message) {
        return new PlatformWebError("AUTHENTICATION_FAILED", 401, message);
    }

    public static PlatformWebError accessDenied(String message) {
        return new PlatformWebError("ACCESS_DENIED", 403, message);
    }

    public static PlatformWebError platformConfiguration(String message) {
        return new PlatformWebError("PLATFORM_CONFIGURATION", 409, message);
    }
}
