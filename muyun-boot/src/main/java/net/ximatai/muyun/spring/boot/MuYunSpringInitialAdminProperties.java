package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.iam.initialdata.PlatformInitialAdminSettings;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MuYunSpringInitialAdminProperties implements PlatformInitialAdminSettings {
    @ConfigProperty(name = "muyun.initial-admin.initial-password", defaultValue = "admin123")
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
