package net.ximatai.muyun.spring.boot.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("muyun.web.cors")
public class MuYunSpringCorsProperties {
    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://127.0.0.1:5173",
            "http://localhost:5173"
    ));

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? new ArrayList<>() : new ArrayList<>(allowedOrigins);
    }
}
