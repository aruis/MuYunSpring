package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;

public interface FieldReadAbility {
    default FieldReadPolicy fieldReadPolicy(ActionExecutionContext actionContext) {
        return FieldReadPolicy.allReadable();
    }
}
