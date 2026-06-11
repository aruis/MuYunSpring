package net.ximatai.muyun.spring.platform.ui;

public record PlatformTaskBlock(
        String uiConfigId,
        String key,
        String title,
        PlatformTaskCheckType checkType,
        String associationViewCode,
        String queryTemplateId,
        String externalRecordIdKey,
        String diagnosticPath
) {
    public PlatformTaskBlock {
        uiConfigId = normalize(uiConfigId);
        key = requireText(key, "task block key");
        title = normalize(title);
        checkType = checkType == null ? PlatformTaskCheckType.MANUAL : checkType;
        associationViewCode = normalize(associationViewCode);
        queryTemplateId = normalize(queryTemplateId);
        externalRecordIdKey = normalize(externalRecordIdKey);
        diagnosticPath = normalize(diagnosticPath);
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
