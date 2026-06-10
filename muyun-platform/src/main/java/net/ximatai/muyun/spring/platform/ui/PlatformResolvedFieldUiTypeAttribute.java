package net.ximatai.muyun.spring.platform.ui;

public record PlatformResolvedFieldUiTypeAttribute(
        String attributeAlias,
        String title,
        String valueFieldTypeAlias,
        String defaultValue
) {
}
