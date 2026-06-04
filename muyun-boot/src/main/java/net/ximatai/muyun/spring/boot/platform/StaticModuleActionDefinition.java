package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;

public record StaticModuleActionDefinition(
        String actionCode,
        String permissionActionCode,
        String title,
        EntityActionLevel actionLevel,
        EntityActionAccessMode accessMode,
        boolean actionAuth,
        boolean dataAuth,
        ActionDefaultGrantPolicy defaultGrantPolicy
) {
    public StaticModuleActionDefinition {
        actionCode = PlatformNameRules.requireActionCode(actionCode, "actionCode");
        permissionActionCode = permissionActionCode == null || permissionActionCode.isBlank()
                ? actionCode
                : PlatformNameRules.requireActionCode(permissionActionCode, "permissionActionCode");
        title = title == null || title.isBlank() ? actionCode : title.trim();
        actionLevel = actionLevel == null ? EntityActionLevel.ANY : actionLevel;
        accessMode = accessMode == null ? EntityActionAccessMode.AUTH_REQUIRED : accessMode;
        defaultGrantPolicy = defaultGrantPolicy == null ? ActionDefaultGrantPolicy.NONE : defaultGrantPolicy;
    }

    public static StaticModuleActionDefinition recordAction(String actionCode, String title) {
        return new StaticModuleActionDefinition(
                actionCode,
                actionCode,
                title,
                EntityActionLevel.RECORD,
                EntityActionAccessMode.AUTH_REQUIRED,
                true,
                true,
                ActionDefaultGrantPolicy.NONE
        );
    }

    public static StaticModuleActionDefinition platformAction(PlatformAction action) {
        return new StaticModuleActionDefinition(
                action.code(),
                action.permissionActionCode(),
                action.title(),
                toEntityLevel(action.level()),
                EntityActionAccessMode.valueOf(action.accessMode().name()),
                action.actionAuth(),
                action.dataAuth(),
                action.defaultGrantPolicy()
        );
    }

    private static EntityActionLevel toEntityLevel(PlatformActionLevel level) {
        if (level == null) {
            return EntityActionLevel.ANY;
        }
        return switch (level) {
            case LIST -> EntityActionLevel.LIST;
            case RECORD -> EntityActionLevel.RECORD;
            case BATCH -> EntityActionLevel.BATCH;
            case DEFAULT, ANY -> EntityActionLevel.ANY;
        };
    }
}
