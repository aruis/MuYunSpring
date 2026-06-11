package net.ximatai.muyun.spring.platform.ui;

public record PlatformTaskCheckBlock(
        PlatformTaskCheckType checkType,
        String associationViewCode,
        String queryTemplateId,
        String externalRecordIdKey,
        String targetModuleAlias,
        String generationRuleId,
        Integer expectedCount,
        String diagnosticPath
) {
    public PlatformTaskCheckBlock {
        checkType = checkType == null ? PlatformTaskCheckType.MANUAL : checkType;
        associationViewCode = normalize(associationViewCode);
        queryTemplateId = normalize(queryTemplateId);
        externalRecordIdKey = normalize(externalRecordIdKey);
        targetModuleAlias = normalize(targetModuleAlias);
        generationRuleId = normalize(generationRuleId);
        expectedCount = expectedCount == null || expectedCount <= 0 ? 1 : expectedCount;
        diagnosticPath = normalize(diagnosticPath);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
