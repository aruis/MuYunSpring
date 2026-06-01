package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;

public record DynamicActionDescriptor(
        String code,
        DynamicActionKind kind,
        String title,
        boolean enabled,
        EntityActionLevel level,
        String permissionCode,
        boolean availabilityCondition,
        String unavailableMessage
) {
    public DynamicActionDescriptor(String code, DynamicActionKind kind, String title) {
        this(code, kind, title, true, EntityActionLevel.NORMAL, null);
    }

    public DynamicActionDescriptor(String code,
                                   DynamicActionKind kind,
                                   String title,
                                   boolean enabled,
                                   EntityActionLevel level,
                                   String permissionCode) {
        this(code, kind, title, enabled, level, permissionCode, false, null);
    }
}
