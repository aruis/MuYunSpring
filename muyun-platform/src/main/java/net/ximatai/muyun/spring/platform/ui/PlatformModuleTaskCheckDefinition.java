package net.ximatai.muyun.spring.platform.ui;

public record PlatformModuleTaskCheckDefinition(
        String taskCode,
        PlatformTaskCheckType checkType,
        String associationViewCode,
        String queryTemplateId,
        String externalRecordIdKey,
        String targetModuleAlias,
        String generationRuleId,
        Integer expectedCount,
        String diagnosticPath
) {
    public PlatformModuleTaskCheckDefinition {
        taskCode = normalize(taskCode);
        checkType = checkType == null ? PlatformTaskCheckType.MANUAL : checkType;
        associationViewCode = normalize(associationViewCode);
        queryTemplateId = normalize(queryTemplateId);
        externalRecordIdKey = normalize(externalRecordIdKey);
        targetModuleAlias = normalize(targetModuleAlias);
        generationRuleId = normalize(generationRuleId);
        expectedCount = expectedCount == null || expectedCount <= 0 ? 1 : expectedCount;
        diagnosticPath = normalize(diagnosticPath);
    }

    PlatformTaskCheckBlock toBlock() {
        return new PlatformTaskCheckBlock(checkType, associationViewCode, queryTemplateId, externalRecordIdKey,
                targetModuleAlias, generationRuleId, expectedCount, diagnosticPath);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
