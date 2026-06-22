package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.spring.common.exception.AuthenticationFailedException;
import net.ximatai.muyun.spring.common.exception.AuthenticationRequiredException;
import net.ximatai.muyun.spring.common.exception.PlatformAccessDeniedException;
import net.ximatai.muyun.spring.common.exception.PlatformConfigurationException;
import net.ximatai.muyun.spring.common.exception.PlatformErrorCodes;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class PlatformWebExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(PlatformWebExceptionHandler.class);

    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<PlatformWebError> handleAuthenticationRequired(AuthenticationRequiredException exception) {
        return platformError(exception);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<PlatformWebError> handleAuthenticationFailed(AuthenticationFailedException exception) {
        return platformError(exception);
    }

    @ExceptionHandler(PlatformAccessDeniedException.class)
    public ResponseEntity<PlatformWebError> handleAccessDenied(PlatformAccessDeniedException exception) {
        return platformError(exception);
    }

    @ExceptionHandler(PlatformConfigurationException.class)
    public ResponseEntity<PlatformWebError> handlePlatformConfiguration(PlatformConfigurationException exception) {
        return platformError(exception);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<PlatformWebError> handleBadRequest(Exception exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(PlatformWebError.of(PlatformErrorCodes.VALIDATION_FAILED, 400, exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageConversionException.class)
    public ResponseEntity<PlatformWebError> handleMessageConversion(HttpMessageConversionException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(PlatformWebError.of(PlatformErrorCodes.VALIDATION_FAILED, 400, rootMessage(exception)));
    }

    @ExceptionHandler(ModuleDefinitionException.class)
    public ResponseEntity<PlatformWebError> handleModuleDefinition(ModuleDefinitionException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(PlatformWebError.of(PlatformErrorCodes.VALIDATION_FAILED, 400, exception.getMessage()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<PlatformWebError> handleNoHandler(NoHandlerFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(PlatformWebError.of(PlatformErrorCodes.RESOURCE_NOT_FOUND, 404, exception.getMessage()));
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<PlatformWebError> handleOptimisticLock(OptimisticLockException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(PlatformWebError.of(PlatformErrorCodes.CONFLICT_VERSION, 409, exception.getMessage()));
    }

    @ExceptionHandler(DynamicActionExecutionException.class)
    public ResponseEntity<PlatformWebError> handleDynamicActionFailure(DynamicActionExecutionException exception) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (exception.failureStage() != null) {
            details.put("failureStage", exception.failureStage());
        }
        if (exception.context() != null) {
            details.put("context", dynamicActionContext(exception.context()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(PlatformWebError.of("DYNAMIC_ACTION_FAILED", 400, exception.getMessage(), details));
    }

    @ExceptionHandler(PlatformException.class)
    public ResponseEntity<PlatformWebError> handlePlatformException(PlatformException exception) {
        return platformError(exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<PlatformWebError> handleUnexpected(Exception exception) {
        PlatformWebError error = PlatformWebError.of(PlatformErrorCodes.INTERNAL_ERROR, 500,
                "Internal server error");
        log.error("Unhandled platform web exception, traceId={}", error.traceId(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private ResponseEntity<PlatformWebError> platformError(PlatformException exception) {
        return ResponseEntity.status(exception.httpStatus()).body(PlatformWebError.of(exception));
    }

    private String rootMessage(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? exception.getMessage() : current.getMessage();
    }

    private Map<String, Object> dynamicActionContext(DynamicActionExecutionContext context) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("moduleAlias", context.moduleAlias());
        value.put("actionCode", context.actionCode());
        if (context.action() != null) {
            value.put("actionLevel", context.action().actionLevel().name());
            value.put("executorType", context.action().executorType().name());
        }
        if (context.recordId() != null) {
            value.put("recordId", context.recordId());
        }
        if (context.traceId() != null) {
            value.put("traceId", context.traceId());
        }
        return value;
    }
}
