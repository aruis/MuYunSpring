package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

public record StaticModuleReferenceDefinition(String code,
                                              String sourceField,
                                              String targetModuleAlias,
                                              String targetField) {
    public StaticModuleReferenceDefinition {
        code = PlatformNameRules.requireIdentifier(code, "referenceCode");
        sourceField = PlatformNameRules.requireFieldName(sourceField, "referenceSourceField");
        targetModuleAlias = PlatformNameRules.requireModuleAlias(targetModuleAlias);
        targetField = targetField == null || targetField.isBlank() ? "id" : targetField.trim();
        targetField = PlatformNameRules.requireFieldName(targetField, "referenceTargetField");
        if (!"id".equals(targetField)) {
            throw new IllegalArgumentException("static module reference targetField currently only supports id: "
                    + code + "." + targetField);
        }
    }
}
