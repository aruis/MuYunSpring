package net.ximatai.muyun.spring.dynamic.runtime;

public record DynamicActionDialog(
        String dialogKey,
        String title,
        String actionCode,
        String submitActionCode,
        String submitPath,
        String recordId,
        boolean refreshOnSuccess,
        String redirectTo,
        DynamicActionRefreshStrategy refreshStrategy
) {
    public DynamicActionDialog(String dialogKey, String title) {
        this(dialogKey, title, null, null, null, null, false, null, null);
    }

    public DynamicActionDialog(String dialogKey,
                               String title,
                               String actionCode,
                               String submitActionCode,
                               String submitPath,
                               String recordId,
                               boolean refreshOnSuccess,
                               String redirectTo) {
        this(dialogKey, title, actionCode, submitActionCode, submitPath, recordId,
                refreshOnSuccess, redirectTo, null);
    }

    public DynamicActionDialog {
        if (dialogKey == null || dialogKey.isBlank()) {
            throw new IllegalArgumentException("dynamic action dialogKey must not be blank");
        }
        dialogKey = dialogKey.trim();
        title = title == null || title.isBlank() ? null : title.trim();
        actionCode = actionCode == null || actionCode.isBlank() ? null : actionCode.trim();
        submitActionCode = submitActionCode == null || submitActionCode.isBlank() ? null : submitActionCode.trim();
        submitPath = submitPath == null || submitPath.isBlank() ? null : submitPath.trim();
        recordId = recordId == null || recordId.isBlank() ? null : recordId.trim();
        redirectTo = redirectTo == null || redirectTo.isBlank() ? null : redirectTo.trim();
        refreshStrategy = refreshStrategy == null
                ? refreshOnSuccess ? DynamicActionRefreshStrategy.listAndDetail() : DynamicActionRefreshStrategy.none()
                : refreshStrategy;
        refreshOnSuccess = refreshOnSuccess || refreshStrategy.active();
    }
}
