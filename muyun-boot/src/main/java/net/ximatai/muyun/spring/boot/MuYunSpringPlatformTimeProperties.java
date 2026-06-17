package net.ximatai.muyun.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("muyun.platform.time")
public class MuYunSpringPlatformTimeProperties {
    private String defaultZoneId;

    public String getDefaultZoneId() {
        return defaultZoneId;
    }

    public void setDefaultZoneId(String defaultZoneId) {
        this.defaultZoneId = defaultZoneId;
    }
}
