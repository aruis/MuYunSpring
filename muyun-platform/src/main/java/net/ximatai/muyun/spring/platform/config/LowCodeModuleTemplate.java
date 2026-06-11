package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

public record LowCodeModuleTemplate(
        String templateAlias,
        String title,
        LowCodeModulePackage basePackage
) {
    public LowCodeModuleTemplate {
        templateAlias = PlatformNameRules.requireIdentifier(templateAlias, "templateAlias");
        title = requireText(title, "title");
        if (basePackage == null) {
            throw new IllegalArgumentException("basePackage must not be null");
        }
        if (basePackage.mode() != LowCodePackageMode.TEMPLATE) {
            throw new IllegalArgumentException("template base package must use TEMPLATE mode");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
