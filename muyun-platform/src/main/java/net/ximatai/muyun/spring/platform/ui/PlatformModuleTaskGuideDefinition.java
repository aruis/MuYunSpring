package net.ximatai.muyun.spring.platform.ui;

public record PlatformModuleTaskGuideDefinition(
        String taskCode,
        PlatformModuleTaskGuideType guideType,
        String actionCode,
        String path,
        String moduleAlias,
        String viewCode,
        String fieldName,
        String title
) {
    public PlatformModuleTaskGuideDefinition {
        taskCode = normalize(taskCode);
        guideType = guideType == null ? PlatformModuleTaskGuideType.DIAGNOSTIC_PATH : guideType;
        actionCode = normalize(actionCode);
        path = normalize(path);
        moduleAlias = normalize(moduleAlias);
        viewCode = normalize(viewCode);
        fieldName = normalize(fieldName);
        title = normalize(title);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
