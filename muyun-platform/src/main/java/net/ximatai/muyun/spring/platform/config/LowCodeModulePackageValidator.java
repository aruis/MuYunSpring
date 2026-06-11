package net.ximatai.muyun.spring.platform.config;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class LowCodeModulePackageValidator {
    private static final Set<LowCodePackageBundleType> PAGE_ONLY_ALLOWED = EnumSet.of(
            LowCodePackageBundleType.PAGE,
            LowCodePackageBundleType.INTERACTION,
            LowCodePackageBundleType.ENTRY
    );

    public void validate(LowCodeModulePackage modulePackage) {
        if (modulePackage == null) {
            throw new IllegalArgumentException("low code module package must not be null");
        }
        Map<LowCodePackageBundleType, LowCodeConfigBundle> bundles = requireUniqueBundles(modulePackage);
        requireIncludedBundlesHaveContent(bundles);
        validateMode(modulePackage.mode(), bundles);
        validateDependencies(modulePackage);
    }

    private Map<LowCodePackageBundleType, LowCodeConfigBundle> requireUniqueBundles(LowCodeModulePackage modulePackage) {
        Map<LowCodePackageBundleType, LowCodeConfigBundle> result = new EnumMap<>(LowCodePackageBundleType.class);
        for (LowCodeConfigBundle bundle : modulePackage.bundles()) {
            if (bundle == null) {
                throw new IllegalArgumentException("package bundle must not be null");
            }
            if (result.put(bundle.type(), bundle) != null) {
                throw new IllegalArgumentException("duplicated package bundle: " + bundle.type());
            }
        }
        return result;
    }

    private void requireIncludedBundlesHaveContent(Map<LowCodePackageBundleType, LowCodeConfigBundle> bundles) {
        for (LowCodeConfigBundle bundle : bundles.values()) {
            if (bundle.included() && bundle.content().isEmpty()) {
                throw new IllegalArgumentException("included package bundle must not be empty: " + bundle.type());
            }
        }
    }

    private void validateMode(LowCodePackageMode mode,
                              Map<LowCodePackageBundleType, LowCodeConfigBundle> bundles) {
        if (mode == LowCodePackageMode.PAGE_ONLY) {
            rejectUnexpectedPageOnlyBundles(bundles);
            requireIncluded(bundles, LowCodePackageBundleType.PAGE, "PAGE_ONLY package must include PAGE bundle");
            return;
        }
        requireIncluded(bundles, LowCodePackageBundleType.METADATA,
                mode + " package must include METADATA bundle");
    }

    private void rejectUnexpectedPageOnlyBundles(Map<LowCodePackageBundleType, LowCodeConfigBundle> bundles) {
        for (LowCodeConfigBundle bundle : bundles.values()) {
            if (bundle.included() && !PAGE_ONLY_ALLOWED.contains(bundle.type())) {
                throw new IllegalArgumentException("PAGE_ONLY package cannot include " + bundle.type() + " bundle");
            }
        }
    }

    private void requireIncluded(Map<LowCodePackageBundleType, LowCodeConfigBundle> bundles,
                                 LowCodePackageBundleType type,
                                 String message) {
        LowCodeConfigBundle bundle = bundles.get(type);
        if (bundle == null || !bundle.included()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateDependencies(LowCodeModulePackage modulePackage) {
        for (LowCodePackageDependency dependency : modulePackage.dependencyManifest().dependencies()) {
            if (dependency == null) {
                throw new IllegalArgumentException("package dependency must not be null");
            }
            switch (dependency.type()) {
                case MODULE -> requireText(dependency.moduleAlias(), "MODULE dependency moduleAlias");
                case ACTION -> {
                    requireText(dependency.moduleAlias(), "ACTION dependency moduleAlias");
                    requireText(dependency.alias(), "ACTION dependency actionCode");
                }
                case DICTIONARY -> {
                    requireText(dependency.applicationAlias(), "DICTIONARY dependency applicationAlias");
                    requireText(dependency.alias(), "DICTIONARY dependency alias");
                }
                case WORKFLOW, FILE_SERVICE, EXTERNAL -> requireText(dependency.alias(), dependency.type() + " dependency alias");
                default -> throw new IllegalArgumentException("unsupported dependency type: " + dependency.type());
            }
        }
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
