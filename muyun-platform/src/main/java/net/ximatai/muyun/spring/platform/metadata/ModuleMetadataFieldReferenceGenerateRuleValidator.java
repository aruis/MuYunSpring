package net.ximatai.muyun.spring.platform.metadata;

@FunctionalInterface
public interface ModuleMetadataFieldReferenceGenerateRuleValidator {
    void validateReferenceGenerateRule(String ruleId,
                                       String referenceModuleAlias,
                                       String ownerModuleAlias);
}
