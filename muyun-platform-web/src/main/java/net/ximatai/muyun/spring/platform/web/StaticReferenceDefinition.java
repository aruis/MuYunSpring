package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

public record StaticReferenceDefinition(String code,
                                        String sourceField,
                                        String targetModuleAlias) {
    public StaticReferenceDefinition {
        code = PlatformNameRules.requireIdentifier(code, "referenceCode");
        sourceField = PlatformNameRules.requireFieldName(sourceField, "referenceSourceField");
        targetModuleAlias = PlatformNameRules.requireModuleAlias(targetModuleAlias);
    }
}
