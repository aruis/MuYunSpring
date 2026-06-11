package net.ximatai.muyun.spring.platform.config;

import java.util.List;

public record LowCodePackageDryRunResult(
        LowCodeModulePackage modulePackage,
        LowCodePackageDryRunStatus status,
        LowCodeConfigHealthReport healthReport,
        List<LowCodePackageImportConflict> conflicts
) {
    public LowCodePackageDryRunResult {
        if (modulePackage == null) {
            throw new IllegalArgumentException("modulePackage must not be null");
        }
        conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
        status = statusOf(healthReport, conflicts);
    }

    public boolean blocked() {
        return status == LowCodePackageDryRunStatus.BLOCKED;
    }

    private static LowCodePackageDryRunStatus statusOf(LowCodeConfigHealthReport healthReport,
                                                       List<LowCodePackageImportConflict> conflicts) {
        if (healthReport != null && healthReport.status() == LowCodeConfigHealthStatus.FAIL) {
            return LowCodePackageDryRunStatus.BLOCKED;
        }
        boolean hasWarn = healthReport != null && healthReport.status() == LowCodeConfigHealthStatus.WARN;
        for (LowCodePackageImportConflict conflict : conflicts) {
            if (conflict.severity() == LowCodePackageConflictSeverity.ERROR) {
                return LowCodePackageDryRunStatus.BLOCKED;
            }
            if (conflict.severity() == LowCodePackageConflictSeverity.WARN) {
                hasWarn = true;
            }
        }
        return hasWarn ? LowCodePackageDryRunStatus.WARN : LowCodePackageDryRunStatus.READY;
    }
}
