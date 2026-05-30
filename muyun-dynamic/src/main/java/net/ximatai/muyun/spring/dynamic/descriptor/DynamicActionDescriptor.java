package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;

public record DynamicActionDescriptor(
        String code,
        DynamicActionKind kind,
        String title,
        boolean enabled,
        EntityActionLevel level,
        String permissionCode
) {
    public DynamicActionDescriptor(String code, DynamicActionKind kind, String title) {
        this(code, kind, title, true, EntityActionLevel.NORMAL, null);
    }
}
