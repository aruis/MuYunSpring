package net.ximatai.muyun.spring.platform.code;

public record CodeRuntimeResult(
        String value,
        String ruleId,
        String moduleMetadataFieldId,
        String metadataFieldId,
        String fieldName,
        CodeFieldRole fieldRole,
        String resolvedOrganizationId,
        String basisKey,
        String periodKey,
        Long sequenceValue,
        boolean bound,
        String sourceRecordId
) {
    public CodeRuntimeResult(String value,
                             String ruleId,
                             String metadataFieldId,
                             String fieldName,
                             CodeFieldRole fieldRole,
                             String resolvedOrganizationId,
                             String basisKey,
                             String periodKey,
                             Long sequenceValue,
                             boolean bound,
                             String sourceRecordId) {
        this(value, ruleId, null, metadataFieldId, fieldName, fieldRole, resolvedOrganizationId, basisKey, periodKey,
                sequenceValue, bound, sourceRecordId);
    }

    public static CodeRuntimeResult generated(GenerateCodeResult result, boolean bound, String sourceRecordId) {
        return new CodeRuntimeResult(
                result.value(),
                result.ruleId(),
                result.moduleMetadataFieldId(),
                result.metadataFieldId(),
                result.fieldName(),
                result.fieldRole(),
                result.resolvedOrganizationId(),
                result.basisKey(),
                result.periodKey(),
                result.sequenceValue(),
                bound,
                sourceRecordId
        );
    }

    public static CodeRuntimeResult bound(CodeLedgerEntry entry, CodeRule rule) {
        return new CodeRuntimeResult(
                entry.getCodeValue(),
                entry.getRuleId(),
                rule == null ? null : rule.getModuleMetadataFieldId(),
                rule == null ? null : rule.getMetadataFieldId(),
                entry.getFieldName(),
                rule == null ? null : rule.getFieldRole(),
                null,
                entry.getBasisKey(),
                entry.getPeriodKey(),
                null,
                true,
                entry.getSourceRecordId()
        );
    }
}
