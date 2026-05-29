package net.ximatai.muyun.spring.common.util;

/**
 * Compatibility facade for alias-specific callers. New platform naming rules
 * should be added to {@link PlatformNameRules}.
 */
@Deprecated
public final class PlatformAliasRules {
    public static final int ALIAS_SEGMENT_MAX_LENGTH = PlatformNameRules.IDENTIFIER_MAX_LENGTH;
    public static final int MODULE_ALIAS_MAX_LENGTH = PlatformNameRules.MODULE_ALIAS_MAX_LENGTH;

    private PlatformAliasRules() {
    }

    public static String requireApplicationAlias(String value) {
        return PlatformNameRules.requireApplicationAlias(value);
    }

    public static String requireModuleAlias(String value) {
        return PlatformNameRules.requireModuleAlias(value);
    }

    public static String requireModuleAliasInApplication(String moduleAlias, String applicationAlias) {
        return PlatformNameRules.requireModuleAliasInApplication(moduleAlias, applicationAlias);
    }

    public static boolean isIdentifier(String value) {
        return PlatformNameRules.isIdentifier(value);
    }
}
