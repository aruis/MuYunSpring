package net.ximatai.muyun.spring.boot.configuration.runtime;

import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeMode;
import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeModeProvider;

/** 以 {@code muyun.runtime.mode} 为来源的运行模式提供者。 */
public class PropertiesPlatformRuntimeModeProvider implements PlatformRuntimeModeProvider {
    private final MuYunSpringRuntimeProperties properties;

    public PropertiesPlatformRuntimeModeProvider(MuYunSpringRuntimeProperties properties) {
        this.properties = properties;
    }

    @Override
    /** 属性对象缺失时仍坚持生产模式，保证治理默认值保守。 */
    public PlatformRuntimeMode currentMode() {
        return properties == null ? PlatformRuntimeMode.PRODUCTION : properties.getMode();
    }
}
