package net.ximatai.muyun.spring.boot;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MuYunSpringPlatformTimeProperties {
    @ConfigProperty(name = "muyun.platform.time.default-zone-id", defaultValue = "")
    private String defaultZoneId;

    public String getDefaultZoneId() {
        return defaultZoneId;
    }

    public void setDefaultZoneId(String defaultZoneId) {
        this.defaultZoneId = defaultZoneId;
    }
}
