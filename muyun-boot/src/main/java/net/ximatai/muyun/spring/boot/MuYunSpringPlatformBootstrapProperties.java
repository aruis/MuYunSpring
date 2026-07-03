package net.ximatai.muyun.spring.boot;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MuYunSpringPlatformBootstrapProperties {
    @ConfigProperty(name = "muyun.platform-bootstrap.enabled", defaultValue = "true")
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
