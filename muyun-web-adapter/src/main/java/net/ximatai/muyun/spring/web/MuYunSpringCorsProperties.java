package net.ximatai.muyun.spring.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("muyun.web.cors")
public class MuYunSpringCorsProperties {
    /**
     * Cross-origin access is opt-in. Local development origins belong in the local profile,
     * rather than becoming an implicit production deployment policy.
     */
    private List<String> allowedOrigins = new ArrayList<>();

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? new ArrayList<>() : new ArrayList<>(allowedOrigins);
    }
}
