package net.ximatai.muyun.spring.platform.code;

public record GenerateCodeResult(
        String value,
        String ruleId,
        String moduleMetadataFieldId,
        String metadataFieldId,
        String fieldName,
        CodeFieldRole fieldRole,
        String resolvedOrganizationId,
        String basisKey,
        String periodKey,
        Long sequenceValue
) {
    public GenerateCodeResult(String value,
                              String ruleId,
                              String metadataFieldId,
                              String fieldName,
                              CodeFieldRole fieldRole,
                              String resolvedOrganizationId,
                              String basisKey,
                              String periodKey,
                              Long sequenceValue) {
        this(value, ruleId, null, metadataFieldId, fieldName, fieldRole, resolvedOrganizationId, basisKey, periodKey,
                sequenceValue);
    }
}
