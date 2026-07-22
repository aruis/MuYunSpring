package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.platform.PlatformAction;

public interface RoleActionGrantVerifier {
    String resolveGrantablePermissionActionCode(String moduleAlias, String actionCode);

    default boolean requiresDataScope(String moduleAlias, String actionCode) {
        return false;
    }

    static RoleActionGrantVerifier platformActionsOnly() {
        return new RoleActionGrantVerifier() {
            @Override
            public String resolveGrantablePermissionActionCode(String moduleAlias, String actionCode) {
                return PlatformAction.fromCode(actionCode)
                        .map(PlatformAction::permissionActionCode)
                        .orElseThrow(() -> new IllegalArgumentException("unsupported actionCode: " + actionCode));
            }

            @Override
            public boolean requiresDataScope(String moduleAlias, String actionCode) {
                return PlatformAction.fromCode(actionCode)
                        .map(PlatformAction::dataAuth)
                        .orElse(false);
            }
        };
    }
}
