package net.ximatai.muyun.spring.dynamic.web;

import net.ximatai.muyun.spring.ability.action.ActionMessage;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionException;
import net.ximatai.muyun.spring.common.exception.PlatformErrorCodes;
import net.ximatai.muyun.spring.web.PlatformWebError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Dynamic runtime errors are mapped at the dynamic HTTP delivery boundary. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DynamicWebExceptionHandler {
    @ExceptionHandler(ModuleDefinitionException.class)
    public ResponseEntity<PlatformWebError> handleModuleDefinition(ModuleDefinitionException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(PlatformWebError.of(
                PlatformErrorCodes.VALIDATION_FAILED, 400, exception.getMessage(),
                ActionMessage.warning(PlatformErrorCodes.VALIDATION_FAILED, exception.getMessage())));
    }

    @ExceptionHandler(DynamicActionExecutionException.class)
    public ResponseEntity<PlatformWebError> handleDynamicActionFailure(DynamicActionExecutionException exception) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (exception.failureStage() != null) details.put("failureStage", exception.failureStage());
        if (exception.context() != null) details.put("context", context(exception.context()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(PlatformWebError.of(
                "DYNAMIC_ACTION_FAILED", 400, exception.getMessage(), details)
                .withActionMessage(ActionMessage.warning("DYNAMIC_ACTION_FAILED", exception.getMessage())));
    }

    private Map<String, Object> context(DynamicActionExecutionContext context) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("moduleAlias", context.moduleAlias());
        value.put("actionCode", context.actionCode());
        if (context.action() != null) {
            value.put("actionLevel", context.action().actionLevel().name());
            value.put("executorType", context.action().executorType().name());
        }
        if (context.recordId() != null) value.put("recordId", context.recordId());
        if (context.traceId() != null) value.put("traceId", context.traceId());
        return value;
    }
}
