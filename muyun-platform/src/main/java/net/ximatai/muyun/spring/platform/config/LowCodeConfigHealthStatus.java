package net.ximatai.muyun.spring.platform.config;

import java.util.List;

public enum LowCodeConfigHealthStatus {
    PASS,
    WARN,
    FAIL;

    public static LowCodeConfigHealthStatus of(List<LowCodeConfigHealthItem> items) {
        if (items == null || items.isEmpty()) {
            return PASS;
        }
        boolean hasWarn = false;
        for (LowCodeConfigHealthItem item : items) {
            if (item != null && item.severity() == LowCodeConfigHealthSeverity.ERROR) {
                return FAIL;
            }
            if (item != null && item.severity() == LowCodeConfigHealthSeverity.WARN) {
                hasWarn = true;
            }
        }
        return hasWarn ? WARN : PASS;
    }
}
