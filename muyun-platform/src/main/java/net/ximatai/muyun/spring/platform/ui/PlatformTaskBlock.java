package net.ximatai.muyun.spring.platform.ui;

public record PlatformTaskBlock(
        String uiConfigId,
        String key,
        String title,
        PlatformTaskCheckType checkType,
        String associationViewCode,
        String queryTemplateId,
        String externalRecordIdKey,
        String diagnosticPath,
        String targetModuleAlias,
        String generationRuleId,
        Integer expectedCount,
        java.util.List<PlatformTaskCheckBlock> checks
) {
    public PlatformTaskBlock(String uiConfigId,
                             String key,
                             String title,
                             PlatformTaskCheckType checkType,
                             String associationViewCode,
                             String queryTemplateId,
                             String externalRecordIdKey,
                             String diagnosticPath) {
        this(uiConfigId, key, title, checkType, associationViewCode, queryTemplateId, externalRecordIdKey,
                diagnosticPath, null, null, null, null);
    }

    public PlatformTaskBlock {
        uiConfigId = normalize(uiConfigId);
        key = requireText(key, "task block key");
        title = normalize(title);
        checkType = checkType == null ? PlatformTaskCheckType.MANUAL : checkType;
        associationViewCode = normalize(associationViewCode);
        queryTemplateId = normalize(queryTemplateId);
        externalRecordIdKey = normalize(externalRecordIdKey);
        diagnosticPath = normalize(diagnosticPath);
        targetModuleAlias = normalize(targetModuleAlias);
        generationRuleId = normalize(generationRuleId);
        expectedCount = expectedCount == null || expectedCount <= 0 ? 1 : expectedCount;
        checks = checks == null || checks.isEmpty()
                ? java.util.List.of(new PlatformTaskCheckBlock(checkType, associationViewCode, queryTemplateId,
                externalRecordIdKey, targetModuleAlias, generationRuleId, expectedCount, diagnosticPath))
                : java.util.List.copyOf(checks);
        if (checkType == PlatformTaskCheckType.MANUAL
                && !checks.isEmpty()
                && checks.getFirst().checkType() != PlatformTaskCheckType.MANUAL) {
            checkType = checks.getFirst().checkType();
        }
    }

    private static String requireText(String value, String name) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
