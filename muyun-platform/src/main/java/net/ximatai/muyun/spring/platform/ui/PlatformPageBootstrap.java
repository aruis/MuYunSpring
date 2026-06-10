package net.ximatai.muyun.spring.platform.ui;

public record PlatformPageBootstrap(
        PlatformPageEntryContext entry,
        PlatformUiClientType clientType,
        PlatformResolvedPageConfig resolvedConfig
) {
}
