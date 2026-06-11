package net.ximatai.muyun.spring.platform.ui;

public record PlatformModuleTaskCheckDetail(
        PlatformTaskCheckType checkType,
        Boolean passed,
        Long actualCount,
        Integer expectedCount,
        String diagnosticPath,
        String message
) {
}
