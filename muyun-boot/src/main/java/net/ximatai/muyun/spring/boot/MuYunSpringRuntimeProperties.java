package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeMode;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MuYunSpringRuntimeProperties {
    @ConfigProperty(name = "muyun.runtime.mode", defaultValue = "PRODUCTION")
    private PlatformRuntimeMode mode = PlatformRuntimeMode.PRODUCTION;

    public PlatformRuntimeMode getMode() {
        return mode;
    }

    public void setMode(PlatformRuntimeMode mode) {
        this.mode = mode == null ? PlatformRuntimeMode.PRODUCTION : mode;
    }
}
