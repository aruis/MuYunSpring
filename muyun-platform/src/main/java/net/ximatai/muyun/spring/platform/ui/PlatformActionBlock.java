package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionRefreshStrategy;

public record PlatformActionBlock(
        String uiConfigId,
        String type,
        String key,
        String actionCode,
        String title,
        String position,
        String targetUiConfigId,
        String submitPath,
        DynamicActionRefreshStrategy refreshStrategy,
        Integer width,
        Integer height
) {
    public PlatformActionBlock(String uiConfigId,
                               String type,
                               String key,
                               String actionCode,
                               String title,
                               String position) {
        this(uiConfigId, type, key, actionCode, title, position, null, null, null, null, null);
    }

    public PlatformActionBlock {
        uiConfigId = normalize(uiConfigId);
        type = requireText(type, "action block type");
        key = normalize(key);
        actionCode = requireText(actionCode, "action block actionCode");
        title = normalize(title);
        position = normalize(position);
        targetUiConfigId = normalize(targetUiConfigId);
        submitPath = normalize(submitPath);
        refreshStrategy = refreshStrategy == null ? DynamicActionRefreshStrategy.none() : refreshStrategy;
        width = positive(width, "action block width");
        height = positive(height, "action block height");
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

    private static Integer positive(Integer value, String name) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
