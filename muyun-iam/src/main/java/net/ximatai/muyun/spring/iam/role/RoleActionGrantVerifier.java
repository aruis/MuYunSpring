package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.platform.PlatformAction;

public interface RoleActionGrantVerifier {
    void requireGrantable(String moduleAlias, String actionCode);

    static RoleActionGrantVerifier platformActionsOnly() {
        return (moduleAlias, actionCode) -> {
            if (PlatformAction.fromCode(actionCode).isEmpty()) {
                throw new IllegalArgumentException("unsupported actionCode: " + actionCode);
            }
        };
    }
}
