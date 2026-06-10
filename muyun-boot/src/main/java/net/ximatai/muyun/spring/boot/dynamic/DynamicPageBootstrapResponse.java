package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.platform.ui.PlatformPageEntryContext;
import net.ximatai.muyun.spring.platform.ui.PlatformResolvedPageConfig;
import net.ximatai.muyun.spring.platform.ui.PlatformUiClientType;

public record DynamicPageBootstrapResponse(
        PlatformPageEntryContext entry,
        PlatformUiClientType clientType,
        DynamicModuleDescriptor moduleDescriptor,
        String mainEntityAlias,
        PlatformResolvedPageConfig resolvedConfig,
        String openApiPath
) {
}
