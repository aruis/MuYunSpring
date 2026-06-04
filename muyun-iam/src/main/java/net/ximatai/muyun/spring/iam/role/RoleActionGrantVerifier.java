package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.platform.PlatformAction;

public interface RoleActionGrantVerifier {
    String resolveGrantablePermissionActionCode(String moduleAlias, String actionCode);

    static RoleActionGrantVerifier platformActionsOnly() {
        return (moduleAlias, actionCode) -> PlatformAction.fromCode(actionCode)
                .map(PlatformAction::permissionActionCode)
                .orElseThrow(() -> new IllegalArgumentException("unsupported actionCode: " + actionCode));
    }
}
