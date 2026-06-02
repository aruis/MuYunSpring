package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionStyle;

public record DynamicActionDescriptor(
        String code,
        DynamicActionKind kind,
        String title,
        boolean enabled,
        EntityActionStyle style,
        EntityActionLevel actionLevel,
        EntityActionCategory category,
        EntityActionAccessMode accessMode,
        boolean actionAuth,
        boolean dataAuth,
        String authInheritActionCode,
        boolean availabilityCondition,
        String unavailableMessage,
        EntityActionExecutorType executorType,
        String executorKey
) {
}
