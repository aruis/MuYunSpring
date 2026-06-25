package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("muyun.runtime")
public class MuYunSpringRuntimeProperties {
    private PlatformRuntimeMode mode = PlatformRuntimeMode.PRODUCTION;

    public PlatformRuntimeMode getMode() {
        return mode;
    }

    public void setMode(PlatformRuntimeMode mode) {
        this.mode = mode == null ? PlatformRuntimeMode.PRODUCTION : mode;
    }
}
