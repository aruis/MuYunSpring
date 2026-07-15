package net.ximatai.muyun.spring.ability.action;

import net.ximatai.muyun.spring.common.exception.ErrorScope;
import net.ximatai.muyun.spring.common.exception.ErrorTarget;

import java.util.List;
import java.util.Map;

public final class BusinessExceptions {
    private BusinessExceptions() {
    }

    public static BusinessException warning(String code, String message) {
        return new BusinessException(code, message, ActionMessageType.WARNING);
    }

    public static BusinessException error(String code, String message) {
        return new BusinessException(code, message, ActionMessageType.ERROR);
    }

    public static BusinessException conflict(String code, String message) {
        return new BusinessException(code, 409, message, ActionMessageType.WARNING);
    }

    public static BusinessException warning(String code,
                                            String message,
                                            ErrorScope scope,
                                            List<ErrorTarget> targets,
                                            Map<String, Object> details) {
        return new BusinessException(code, 422, message, ActionMessageType.WARNING, scope, targets, details);
    }
}
