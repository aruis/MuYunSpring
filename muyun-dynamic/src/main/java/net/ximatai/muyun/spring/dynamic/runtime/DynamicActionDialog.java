package net.ximatai.muyun.spring.dynamic.runtime;

public record DynamicActionDialog(
        String dialogKey,
        String title
) {
    public DynamicActionDialog {
        if (dialogKey == null || dialogKey.isBlank()) {
            throw new IllegalArgumentException("dynamic action dialogKey must not be blank");
        }
        dialogKey = dialogKey.trim();
        title = title == null || title.isBlank() ? null : title.trim();
    }
}
