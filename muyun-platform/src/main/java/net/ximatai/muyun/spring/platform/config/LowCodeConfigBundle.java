package net.ximatai.muyun.spring.platform.config;

import java.util.Map;

public record LowCodeConfigBundle(
        LowCodePackageBundleType type,
        boolean included,
        Map<String, Object> content
) {
    public LowCodeConfigBundle {
        if (type == null) {
            throw new IllegalArgumentException("bundle type must not be null");
        }
        content = content == null ? Map.of() : Map.copyOf(content);
    }

    public static LowCodeConfigBundle omitted(LowCodePackageBundleType type) {
        return new LowCodeConfigBundle(type, false, Map.of());
    }

    public static LowCodeConfigBundle included(LowCodePackageBundleType type, Map<String, Object> content) {
        return new LowCodeConfigBundle(type, true, content);
    }
}
