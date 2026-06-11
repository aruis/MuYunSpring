package net.ximatai.muyun.spring.platform.ui;

public record PlatformModuleTaskStatus(
        String key,
        String title,
        PlatformTaskCheckType checkType,
        PlatformTaskCompletionStatus status,
        Long matchedCount,
        String diagnosticPath,
        String message
) {
}
