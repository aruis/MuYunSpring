package net.ximatai.muyun.spring.platform.ui;

import java.util.List;

public record PlatformModuleTaskCheckResult(
        Boolean passed,
        List<PlatformModuleTaskStatus> tasks
) {
    public PlatformModuleTaskCheckResult {
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }

    public static PlatformModuleTaskCheckResult of(List<PlatformModuleTaskStatus> tasks) {
        List<PlatformModuleTaskStatus> safeTasks = tasks == null ? List.of() : tasks;
        if (safeTasks.isEmpty()) {
            return new PlatformModuleTaskCheckResult(null, safeTasks);
        }
        boolean hasUnknown = false;
        for (PlatformModuleTaskStatus task : safeTasks) {
            if (task == null || task.passed() == null) {
                hasUnknown = true;
                continue;
            }
            if (!task.passed()) {
                return new PlatformModuleTaskCheckResult(false, safeTasks);
            }
        }
        return new PlatformModuleTaskCheckResult(hasUnknown ? null : true, safeTasks);
    }
}
