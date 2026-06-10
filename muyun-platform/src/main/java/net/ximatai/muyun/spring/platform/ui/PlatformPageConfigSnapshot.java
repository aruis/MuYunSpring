package net.ximatai.muyun.spring.platform.ui;

import java.util.List;

public record PlatformPageConfigSnapshot(
        String moduleAlias,
        List<PlatformUiSet> uiSets,
        List<PlatformUiConfig> uiConfigs,
        List<PlatformUiConfigField> uiFields,
        List<PlatformQueryTemplate> queryTemplates,
        List<PlatformQueryItem> queryItems
) {
}
