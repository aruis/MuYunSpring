package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.module.ModuleKind;

import java.util.List;

public record ResolvedModuleUiDescriptor(String schemaVersion,
                                         String moduleAlias,
                                         ModuleKind moduleKind,
                                         String title,
                                         List<ResolvedViewDescriptor> views) {
    public static final String SCHEMA_VERSION = "module-ui.v1";

    public ResolvedModuleUiDescriptor {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        moduleAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        title = title == null || title.isBlank() ? null : title.trim();
        views = views == null ? List.of() : List.copyOf(views);
    }

    public ResolvedModuleUiDescriptor(String moduleAlias,
                                      List<ResolvedViewDescriptor> views) {
        this(SCHEMA_VERSION, moduleAlias, null, null, views);
    }
}
