package net.ximatai.muyun.spring.boot.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.spring.common.exception.AuthenticationFailedException;
import net.ximatai.muyun.spring.common.exception.AuthenticationRequiredException;
import net.ximatai.muyun.spring.common.exception.PlatformAccessDeniedException;
import net.ximatai.muyun.spring.common.exception.PlatformConfigurationException;
import net.ximatai.muyun.spring.common.exception.PlatformErrorCodes;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionException;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class PlatformWebExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(PlatformWebExceptionHandler.class);

    @ServerExceptionMapper
    public Response handleAuthenticationRequired(AuthenticationRequiredException exception) {
        return platformError(exception);
    }

    @ServerExceptionMapper
    public Response handleAuthenticationFailed(AuthenticationFailedException exception) {
        return platformError(exception);
    }

    @ServerExceptionMapper
    public Response handleAccessDenied(PlatformAccessDeniedException exception) {
        return platformError(exception);
    }

    @ServerExceptionMapper
    public Response handlePlatformConfiguration(PlatformConfigurationException exception) {
        return platformError(exception);
    }

    @ServerExceptionMapper
    public Response handleBadRequest(IllegalArgumentException exception) {
        return error(400, PlatformWebError.of(PlatformErrorCodes.VALIDATION_FAILED, 400, exception.getMessage()));
    }

    @ServerExceptionMapper
    public Response handleBadRequest(BadRequestException exception) {
        return error(400, PlatformWebError.of(PlatformErrorCodes.VALIDATION_FAILED, 400, exception.getMessage()));
    }

    @ServerExceptionMapper
    public Response handleMethodNotAllowed(NotAllowedException exception) {
        return error(405, PlatformWebError.of(PlatformErrorCodes.VALIDATION_FAILED, 405, exception.getMessage()));
    }

    @ServerExceptionMapper
    public Response handleNotFound(NotFoundException exception) {
        return error(404, PlatformWebError.of(PlatformErrorCodes.RESOURCE_NOT_FOUND, 404, exception.getMessage()));
    }

    @ServerExceptionMapper
    public Response handleModuleDefinition(ModuleDefinitionException exception) {
        return error(400, PlatformWebError.of(PlatformErrorCodes.VALIDATION_FAILED, 400, exception.getMessage()));
    }

    @ServerExceptionMapper
    public Response handleOptimisticLock(OptimisticLockException exception) {
        return error(409, PlatformWebError.of(PlatformErrorCodes.CONFLICT_VERSION, 409, exception.getMessage()));
    }

    @ServerExceptionMapper
    public Response handleDynamicActionFailure(DynamicActionExecutionException exception) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (exception.failureStage() != null) {
            details.put("failureStage", exception.failureStage());
        }
        if (exception.context() != null) {
            details.put("context", dynamicActionContext(exception.context()));
        }
        return error(400, PlatformWebError.of("DYNAMIC_ACTION_FAILED", 400, exception.getMessage(), details));
    }

    @ServerExceptionMapper
    public Response handlePlatformException(PlatformException exception) {
        return platformError(exception);
    }

    @ServerExceptionMapper
    public Response handleUnexpected(Exception exception) {
        PlatformWebError error = PlatformWebError.of(PlatformErrorCodes.INTERNAL_ERROR, 500,
                "Internal server error");
        log.error("Unhandled platform web exception, traceId={}", error.traceId(), exception);
        return error(500, error);
    }

    private Response platformError(PlatformException exception) {
        return error(exception.httpStatus(), PlatformWebError.of(exception));
    }

    private Response error(int status, PlatformWebError error) {
        return Response.status(status).entity(error).build();
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
