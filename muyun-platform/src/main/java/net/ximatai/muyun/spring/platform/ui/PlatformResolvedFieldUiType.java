package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.spring.dynamic.metadata.ViewControlType;

import java.util.List;

public record PlatformResolvedFieldUiType(
        String alias,
        String title,
        String defaultFieldTypeAlias,
        ViewControlType controlType,
        String icon,
        List<PlatformResolvedFieldUiTypeAttribute> attributes,
        List<PlatformResolvedFieldUiTypeFieldMapping> fieldMappings
) {
    public PlatformResolvedFieldUiType {
        attributes = attributes == null ? List.of() : List.copyOf(attributes);
        fieldMappings = fieldMappings == null ? List.of() : List.copyOf(fieldMappings);
    }
}
