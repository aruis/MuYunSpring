package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record LowCodeModulePackage(
        String protocolVersion,
        LowCodePackageMode mode,
        String applicationAlias,
        String moduleAlias,
        List<LowCodeConfigBundle> bundles,
        LowCodePackageDependencyManifest dependencyManifest,
        LowCodePackagePublishManifest publishManifest
) {
    public LowCodeModulePackage {
        protocolVersion = requireText(protocolVersion, "protocolVersion");
        mode = mode == null ? LowCodePackageMode.MODULE_FULL : mode;
        applicationAlias = PlatformNameRules.requireApplicationAlias(applicationAlias);
        moduleAlias = PlatformNameRules.requireModuleAliasInApplication(moduleAlias, applicationAlias);
        bundles = bundles == null ? List.of() : List.copyOf(bundles);
        dependencyManifest = dependencyManifest == null
                ? LowCodePackageDependencyManifest.empty()
                : dependencyManifest;
        publishManifest = publishManifest == null
                ? LowCodePackagePublishManifest.draft(protocolVersion)
                : publishManifest;
    }

    public Map<LowCodePackageBundleType, LowCodeConfigBundle> bundleMap() {
        Map<LowCodePackageBundleType, LowCodeConfigBundle> result = new EnumMap<>(LowCodePackageBundleType.class);
        for (LowCodeConfigBundle bundle : bundles) {
            if (result.put(bundle.type(), bundle) != null) {
                throw new IllegalArgumentException("duplicated package bundle: " + bundle.type());
            }
        }
        return Map.copyOf(result);
    }

    public boolean includes(LowCodePackageBundleType type) {
        LowCodeConfigBundle bundle = bundleMap().get(type);
        return bundle != null && bundle.included();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
