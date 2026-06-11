package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

public record LowCodePackageDependency(
        LowCodePackageDependencyType type,
        String applicationAlias,
        String moduleAlias,
        String alias,
        boolean required
) {
    public LowCodePackageDependency {
        if (type == null) {
            throw new IllegalArgumentException("dependency type must not be null");
        }
        applicationAlias = normalize(applicationAlias);
        moduleAlias = normalize(moduleAlias);
        alias = normalize(alias);
        if (applicationAlias != null) {
            PlatformNameRules.requireApplicationAlias(applicationAlias);
        }
        if (moduleAlias != null) {
            PlatformNameRules.requireModuleAlias(moduleAlias);
        }
        validateAlias(type, alias);
    }

    public static LowCodePackageDependency module(String moduleAlias) {
        return new LowCodePackageDependency(LowCodePackageDependencyType.MODULE, null, moduleAlias, null, true);
    }

    public static LowCodePackageDependency action(String moduleAlias, String actionCode) {
        return new LowCodePackageDependency(LowCodePackageDependencyType.ACTION, null, moduleAlias, actionCode, true);
    }

    public static LowCodePackageDependency dictionary(String applicationAlias, String dictionaryAlias) {
        return new LowCodePackageDependency(LowCodePackageDependencyType.DICTIONARY,
                applicationAlias, null, dictionaryAlias, true);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void validateAlias(LowCodePackageDependencyType type, String alias) {
        if (alias == null) {
            return;
        }
        if (type == LowCodePackageDependencyType.ACTION) {
            PlatformNameRules.requireActionCode(alias, "dependency actionCode");
            return;
        }
        PlatformNameRules.requireIdentifier(alias, "dependency alias");
    }
}
