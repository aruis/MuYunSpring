package net.ximatai.muyun.spring.platform.ui;

public record PlatformPageBootstrap(
        PlatformPageEntryContext entry,
        PlatformPageConfigSnapshot pageConfig,
        PlatformResolvedPageConfig resolvedConfig
) {
    public PlatformPageBootstrap(PlatformPageEntryContext entry, PlatformPageConfigSnapshot pageConfig) {
        this(entry, pageConfig, PlatformResolvedPageConfig.empty());
    }
}
