package net.ximatai.muyun.spring.boot.web.endpoint;

import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Objects;

/** Source-neutral identity and action semantics for an endpoint accepted by the web runtime. */
public record ResolvedWebEndpoint(
        String endpointId,
        String moduleAlias,
        String abilityCode,
        String operationCode,
        PlatformAction action,
        RequestMethod method,
        String path,
        Source source,
        ActionExecutionPolicy executionPolicy
) {
    public ResolvedWebEndpoint {
        endpointId = requireText(endpointId, "endpointId");
        moduleAlias = requireText(moduleAlias, "moduleAlias");
        abilityCode = requireText(abilityCode, "abilityCode");
        operationCode = requireText(operationCode, "operationCode");
        action = Objects.requireNonNull(action, "action must not be null");
        method = Objects.requireNonNull(method, "method must not be null");
        path = normalizePath(path);
        source = Objects.requireNonNull(source, "source must not be null");
        executionPolicy = Objects.requireNonNull(executionPolicy, "executionPolicy must not be null");
    }

    public ResolvedWebEndpoint(String endpointId,
                               String moduleAlias,
                               String abilityCode,
                               String operationCode,
                               PlatformAction action,
                               RequestMethod method,
                               String path,
                               Source source) {
        this(endpointId, moduleAlias, abilityCode, operationCode, action, method, path, source,
                action.executionPolicy());
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizePath(String value) {
        String path = requireText(value, "path");
        return path.startsWith("/") ? path : "/" + path;
    }

    public enum Source {
        STATIC_ABILITY,
        STATIC_EXPLICIT
    }
}
