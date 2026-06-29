package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.util.List;

public record ResolvedModuleUiDescriptor(String moduleAlias,
                                         List<ResolvedViewDescriptor> views) {
    public ResolvedModuleUiDescriptor {
        moduleAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        views = views == null ? List.of() : List.copyOf(views);
    }
}
