package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.common.exception.AuthenticationFailedException;
import net.ximatai.muyun.spring.common.exception.AuthenticationRequiredException;
import net.ximatai.muyun.spring.common.exception.PlatformAccessDeniedException;
import net.ximatai.muyun.spring.common.exception.PlatformConfigurationException;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PlatformWebExceptionHandler {
    @ExceptionHandler(AuthenticationRequiredException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public PlatformWebError handleAuthenticationRequired(AuthenticationRequiredException exception) {
        return PlatformWebError.authenticationRequired(exception.getMessage());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public PlatformWebError handleAuthenticationFailed(AuthenticationFailedException exception) {
        return PlatformWebError.authenticationFailed(exception.getMessage());
    }

    @ExceptionHandler(PlatformAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public PlatformWebError handleAccessDenied(PlatformAccessDeniedException exception) {
        return PlatformWebError.accessDenied(exception.getMessage());
    }

    @ExceptionHandler(PlatformConfigurationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public PlatformWebError handlePlatformConfiguration(PlatformConfigurationException exception) {
        return PlatformWebError.platformConfiguration(exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public PlatformWebError handleBadRequest(Exception exception) {
        return PlatformWebError.badRequest(exception.getMessage());
    }

    @ExceptionHandler(PlatformException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public PlatformWebError handlePlatformException(PlatformException exception) {
        return PlatformWebError.platformError(exception.getMessage());
    }
}
