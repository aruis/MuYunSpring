package net.ximatai.muyun.spring.platform.config;

public record LowCodePackageDependencyDiagnostic(
        LowCodePackageDependency dependency,
        LowCodePackageConflictType conflictType,
        boolean blocking,
        String targetId,
        String message
) {
}
