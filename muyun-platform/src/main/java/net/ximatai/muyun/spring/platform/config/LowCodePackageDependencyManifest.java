package net.ximatai.muyun.spring.platform.config;

import java.util.List;

public record LowCodePackageDependencyManifest(
        List<LowCodePackageDependency> dependencies
) {
    public LowCodePackageDependencyManifest {
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
    }

    public static LowCodePackageDependencyManifest empty() {
        return new LowCodePackageDependencyManifest(List.of());
    }
}
