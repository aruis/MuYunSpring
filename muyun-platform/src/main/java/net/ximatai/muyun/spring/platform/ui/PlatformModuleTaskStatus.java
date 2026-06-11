package net.ximatai.muyun.spring.platform.ui;

public record PlatformModuleTaskStatus(
        String key,
        String title,
        PlatformTaskCheckType checkType,
        PlatformTaskCompletionStatus status,
        Boolean passed,
        Long matchedCount,
        Integer expectedCount,
        java.util.List<PlatformModuleTaskCheckDetail> checks,
        java.util.List<PlatformModuleTaskGuideDefinition> guides,
        String diagnosticPath,
        String message
) {
    public PlatformModuleTaskStatus {
        checks = checks == null ? java.util.List.of() : java.util.List.copyOf(checks);
        guides = guides == null ? java.util.List.of() : java.util.List.copyOf(guides);
    }

    public PlatformModuleTaskStatus(String key,
                                    String title,
                                    PlatformTaskCheckType checkType,
                                    PlatformTaskCompletionStatus status,
                                    Long matchedCount,
                                    String diagnosticPath,
                                    String message) {
        this(key, title, checkType, status, status == PlatformTaskCompletionStatus.COMPLETE ? true
                        : status == PlatformTaskCompletionStatus.PENDING ? false : null,
                matchedCount, 1, java.util.List.of(), java.util.List.of(), diagnosticPath, message);
    }
}
