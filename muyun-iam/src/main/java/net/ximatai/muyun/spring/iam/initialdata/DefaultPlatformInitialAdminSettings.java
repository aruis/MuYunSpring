package net.ximatai.muyun.spring.iam.initialdata;

final class DefaultPlatformInitialAdminSettings implements PlatformInitialAdminSettings {
    static final DefaultPlatformInitialAdminSettings INSTANCE = new DefaultPlatformInitialAdminSettings();

    private DefaultPlatformInitialAdminSettings() {
    }

    @Override
    public String initialPassword() {
        return "admin123";
    }
}
