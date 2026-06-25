package net.ximatai.muyun.spring.common.runtime;

public interface PlatformRuntimeModeProvider {
    PlatformRuntimeMode currentMode();

    default boolean isDevelopment() {
        return currentMode() == PlatformRuntimeMode.DEVELOPMENT;
    }

    default boolean isProduction() {
        return currentMode() == PlatformRuntimeMode.PRODUCTION;
    }
}
