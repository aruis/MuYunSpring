package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.util.PlatformAliasRules;
import net.ximatai.muyun.spring.common.util.Preconditions;

public record GrantableAction(
        String moduleAlias,
        String actionCode,
        String permissionActionCode,
        String title,
        boolean actionAuth,
        boolean dataAuth
) {
    public GrantableAction {
        moduleAlias = PlatformAliasRules.requireModuleAlias(moduleAlias);
        actionCode = Preconditions.requireText(actionCode, "actionCode");
        permissionActionCode = permissionActionCode == null || permissionActionCode.isBlank()
                ? actionCode
                : Preconditions.requireText(permissionActionCode, "permissionActionCode");
        title = title == null || title.isBlank() ? actionCode : title.trim();
    }

    public static GrantableAction ofPlatformDefaults(String moduleAlias, PlatformAction action) {
        return new GrantableAction(
                moduleAlias,
                action.code(),
                action.permissionActionCode(),
                action.title(),
                action.actionAuth(),
                action.dataAuth()
        );
    }
}
