package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.ViewControlType;

import java.util.List;

public record DynamicViewFieldDescriptor(
        String fieldName,
        String title,
        boolean visible,
        ViewControlType controlType,
        String fieldUiTypeAlias,
        List<DynamicFieldCompanionDescriptor> companions,
        boolean readOnly,
        boolean required
) {
    public DynamicViewFieldDescriptor {
        companions = companions == null ? List.of() : List.copyOf(companions);
    }
}
