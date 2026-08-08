package net.ximatai.muyun.spring.web;

import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.spring.ability.action.ActionMessage;
import net.ximatai.muyun.spring.ability.action.ActionMessageType;
import net.ximatai.muyun.spring.ability.action.BusinessException;
import net.ximatai.muyun.spring.common.exception.AuthenticationFailedException;
import net.ximatai.muyun.spring.common.exception.AuthenticationRequiredException;
import net.ximatai.muyun.spring.common.exception.PlatformAccessDeniedException;
import net.ximatai.muyun.spring.common.exception.PlatformConfigurationException;
import net.ximatai.muyun.spring.common.exception.PlatformErrorCodes;
import net.ximatai.muyun.spring.common.exception.PlatformErrors;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.exception.ErrorTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class PlatformWebExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(PlatformWebExceptionHandler.class);

    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<PlatformWebError> handleAuthenticationRequired(AuthenticationRequiredException exception) {
        return platformError(exception, ActionMessageType.ERROR);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<PlatformWebError> handleAuthenticationFailed(AuthenticationFailedException exception) {
        return platformError(exception, ActionMessageType.ERROR);
    }

    @ExceptionHandler(PlatformAccessDeniedException.class)
    public ResponseEntity<PlatformWebError> handleAccessDenied(PlatformAccessDeniedException exception) {
        return platformError(exception, ActionMessageType.ERROR);
    }

    @ExceptionHandler(PlatformConfigurationException.class)
    public ResponseEntity<PlatformWebError> handlePlatformConfiguration(PlatformConfigurationException exception) {
        return platformError(exception);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<PlatformWebError> handleUnreadableRequestBody(HttpMessageNotReadableException exception) {
        return badRequest("请求体格式错误");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<PlatformWebError> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return badRequest("请求参数格式错误", ErrorTarget.field(exception.getName()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<PlatformWebError> handleMissingRequestParameter(MissingServletRequestParameterException exception) {
        return badRequest("缺少必要请求参数", ErrorTarget.field(exception.getParameterName()));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<PlatformWebError> handleMissingRequestPart(MissingServletRequestPartException exception) {
        return badRequest("缺少必要请求部分", ErrorTarget.field(exception.getRequestPartName()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<PlatformWebError> handleBadRequest(IllegalArgumentException exception) {
        ActionMessage actionMessage = ActionMessage.warning(
                PlatformErrorCodes.VALIDATION_FAILED,
                exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(PlatformWebError.of(PlatformErrorCodes.VALIDATION_FAILED, 400,
                        exception.getMessage(), actionMessage));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<PlatformWebError> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(PlatformWebError.of(PlatformErrorCodes.VALIDATION_FAILED, 405, "不支持的请求方法"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<PlatformWebError> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception) {
        ActionMessage actionMessage = ActionMessage.warning(PlatformErrorCodes.VALIDATION_FAILED, "不支持的请求媒体类型");
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(PlatformWebError.of(PlatformErrorCodes.VALIDATION_FAILED, 415,
                        "不支持的请求媒体类型", actionMessage));
    }

    @ExceptionHandler(HttpMessageConversionException.class)
    public ResponseEntity<PlatformWebError> handleMessageConversion(HttpMessageConversionException exception) {
        return badRequest("请求参数格式错误");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<PlatformWebError> handleNoHandler(NoHandlerFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(PlatformWebError.of(PlatformErrorCodes.RESOURCE_NOT_FOUND, 404, "请求资源不存在"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<PlatformWebError> handleNoResource(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(PlatformWebError.of(PlatformErrorCodes.RESOURCE_NOT_FOUND, 404, "请求资源不存在"));
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<PlatformWebError> handleOptimisticLock(OptimisticLockException exception) {
        ActionMessage actionMessage = ActionMessage.warning(
                PlatformErrorCodes.CONFLICT_VERSION,
                "数据已被更新，请刷新后重试");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(PlatformWebError.of(PlatformErrorCodes.CONFLICT_VERSION, 409,
                        "数据已被更新，请刷新后重试", actionMessage));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<PlatformWebError> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(exception.httpStatus())
                .body(PlatformWebError.of(exception).withActionMessage(exception.actionMessage()));
    }

    @ExceptionHandler(PlatformException.class)
    public ResponseEntity<PlatformWebError> handlePlatformException(PlatformException exception) {
        return platformError(exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<PlatformWebError> handleUnexpected(Exception exception) {
        var databaseViolation = DatabaseConstraintViolationResolver.resolve(exception);
        if (databaseViolation.isPresent()) {
            var violation = databaseViolation.get();
            log.warn("Database constraint violation, traceId={}, endpointId={}",
                    MDC.get("traceId"), MDC.get("endpointId"));
            return badRequest(violation.message(), violation.targets());
        }
        PlatformWebError error = PlatformWebError.of(PlatformErrorCodes.INTERNAL_ERROR, 500,
                "系统暂时不可用，请稍后重试");
        log.error("Unhandled platform web exception, traceId={}, endpointId={}",
                error.traceId(), MDC.get("endpointId"), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private ResponseEntity<PlatformWebError> platformError(PlatformException exception) {
        return ResponseEntity.status(exception.httpStatus()).body(PlatformWebError.of(exception));
    }

    private ResponseEntity<PlatformWebError> platformError(PlatformException exception,
                                                           ActionMessageType messageType) {
        ActionMessage actionMessage = new ActionMessage(exception.code(), exception.getMessage(), messageType,
                exception.messageArgs());
        return ResponseEntity.status(exception.httpStatus())
                .body(PlatformWebError.of(exception).withActionMessage(actionMessage));
    }

    private ResponseEntity<PlatformWebError> badRequest(String message) {
        return badRequest(message, java.util.List.of());
    }

    private ResponseEntity<PlatformWebError> badRequest(String message, ErrorTarget target) {
        return badRequest(message, java.util.List.of(target));
    }

    private ResponseEntity<PlatformWebError> badRequest(String message, java.util.List<ErrorTarget> targets) {
        PlatformException exception = PlatformErrors.badRequest(PlatformErrorCodes.VALIDATION_FAILED, message, targets);
        return platformError(exception, ActionMessageType.WARNING);
    }

}
