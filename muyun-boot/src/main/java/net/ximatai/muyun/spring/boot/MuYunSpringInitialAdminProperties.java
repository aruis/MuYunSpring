package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.iam.initialdata.PlatformInitialAdminSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("muyun.initial-admin")
public class MuYunSpringInitialAdminProperties implements PlatformInitialAdminSettings {
    private String initialPassword = "admin123";

    public String getInitialPassword() {
        return initialPassword;
    }

    @Override
    public String initialPassword() {
        return initialPassword;
    }

    public void setInitialPassword(String initialPassword) {
        if (initialPassword == null || initialPassword.isBlank()) {
            throw new IllegalArgumentException("muyun.initial-admin.initial-password must not be blank");
        }
        this.initialPassword = initialPassword.trim();
    }
}
