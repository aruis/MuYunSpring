package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.ViewControlType;

public record DynamicViewFieldDescriptor(
        String fieldName,
        String title,
        boolean visible,
        ViewControlType controlType,
        String timeZoneField,
        boolean readOnly,
        boolean required
) {
}
