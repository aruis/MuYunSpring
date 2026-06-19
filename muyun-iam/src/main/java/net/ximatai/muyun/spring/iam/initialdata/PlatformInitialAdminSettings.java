package net.ximatai.muyun.spring.iam.initialdata;

public interface PlatformInitialAdminSettings {
    String initialPassword();

    static PlatformInitialAdminSettings defaults() {
        return DefaultPlatformInitialAdminSettings.INSTANCE;
    }
}
