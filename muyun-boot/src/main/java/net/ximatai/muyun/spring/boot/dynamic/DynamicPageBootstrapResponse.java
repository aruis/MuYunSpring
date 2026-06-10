package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigSnapshot;
import net.ximatai.muyun.spring.platform.ui.PlatformPageEntryContext;
import net.ximatai.muyun.spring.platform.ui.PlatformResolvedPageConfig;

public record DynamicPageBootstrapResponse(
        PlatformPageEntryContext entry,
        DynamicModuleDescriptor moduleDescriptor,
        String mainEntityAlias,
        PlatformPageConfigSnapshot pageConfig,
        PlatformResolvedPageConfig resolvedConfig
) {
}
