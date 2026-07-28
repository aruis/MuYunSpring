package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

public record ResolvedUiActionDescriptor(String actionCode,
                                         ResolvedUiActionConfirmationDescriptor confirmation) {
    public ResolvedUiActionDescriptor {
        actionCode = PlatformNameRules.requireActionCode(actionCode, "actionCode");
    }
}
