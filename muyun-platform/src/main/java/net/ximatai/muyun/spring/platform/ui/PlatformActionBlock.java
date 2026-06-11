package net.ximatai.muyun.spring.platform.ui;

public record PlatformActionBlock(
        String uiConfigId,
        String type,
        String key,
        String actionCode,
        String title,
        String position
) {
    public PlatformActionBlock {
        uiConfigId = normalize(uiConfigId);
        type = requireText(type, "action block type");
        key = normalize(key);
        actionCode = requireText(actionCode, "action block actionCode");
        title = normalize(title);
        position = normalize(position);
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
