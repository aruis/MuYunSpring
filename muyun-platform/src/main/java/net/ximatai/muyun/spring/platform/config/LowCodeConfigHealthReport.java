package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.util.List;

public record LowCodeConfigHealthReport(
        String moduleAlias,
        LowCodeConfigHealthStatus status,
        List<LowCodeConfigHealthItem> items
) {
    public LowCodeConfigHealthReport {
        moduleAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        items = items == null ? List.of() : List.copyOf(items);
        status = LowCodeConfigHealthStatus.of(items);
    }

    public static LowCodeConfigHealthReport of(String moduleAlias, List<LowCodeConfigHealthItem> items) {
        return new LowCodeConfigHealthReport(moduleAlias, null, items);
    }

    public boolean passed() {
        return status == LowCodeConfigHealthStatus.PASS;
    }
}
