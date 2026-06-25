package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeMode;
import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeModeProvider;

public class PropertiesPlatformRuntimeModeProvider implements PlatformRuntimeModeProvider {
    private final MuYunSpringRuntimeProperties properties;

    public PropertiesPlatformRuntimeModeProvider(MuYunSpringRuntimeProperties properties) {
        this.properties = properties;
    }

    @Override
    public PlatformRuntimeMode currentMode() {
        return properties == null ? PlatformRuntimeMode.PRODUCTION : properties.getMode();
    }
}
