package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

public record UiActionDefinition(String actionCode,
                                 UiActionConfirmationDefinition confirmation) {
    public UiActionDefinition {
        actionCode = PlatformNameRules.requireActionCode(actionCode, "actionCode");
    }

    public static UiActionDefinition typedTextConfirmation(String actionCode, String requiredField) {
        return new UiActionDefinition(actionCode, new UiActionConfirmationDefinition(requiredField));
    }
}
