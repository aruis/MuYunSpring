package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

public record LowCodeModuleHealthContext(
        String moduleAlias,
        LowCodeModulePackage modulePackage
) {
    public LowCodeModuleHealthContext {
        moduleAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
    }

    public static LowCodeModuleHealthContext ofPackage(LowCodeModulePackage modulePackage) {
        if (modulePackage == null) {
            throw new IllegalArgumentException("modulePackage must not be null");
        }
        return new LowCodeModuleHealthContext(modulePackage.moduleAlias(), modulePackage);
    }
}
