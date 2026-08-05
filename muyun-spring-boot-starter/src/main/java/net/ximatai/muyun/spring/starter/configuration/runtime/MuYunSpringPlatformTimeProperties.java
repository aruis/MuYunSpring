package net.ximatai.muyun.spring.starter.configuration.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 平台业务时间默认配置；领域可通过 {@code BusinessTimeZoneResolver} 追加更细粒度规则。 */
@ConfigurationProperties("muyun.platform.time")
public class MuYunSpringPlatformTimeProperties {
    private String defaultZoneId;

    public String getDefaultZoneId() {
        return defaultZoneId;
    }

    public void setDefaultZoneId(String defaultZoneId) {
        // IANA 时区格式由动态运行时装配阶段统一校验。
        this.defaultZoneId = defaultZoneId;
    }
}
