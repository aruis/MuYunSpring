package net.ximatai.muyun.spring.boot.web.endpoint;

import java.lang.reflect.Method;
import java.util.Objects;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

/** Runtime fact created only after the matching Spring MVC mapping is registered. */
public record RegisteredWebEndpoint(
        ResolvedWebEndpoint definition,
        RequestMappingInfo mapping,
        Object handler,
        Method handlerMethod,
        StaticWebOperationTarget staticTarget
) {
    public RegisteredWebEndpoint {
        definition = Objects.requireNonNull(definition, "definition must not be null");
        mapping = Objects.requireNonNull(mapping, "mapping must not be null");
        handler = Objects.requireNonNull(handler, "handler must not be null");
        handlerMethod = Objects.requireNonNull(handlerMethod, "handlerMethod must not be null");
    }

    public RegisteredWebEndpoint(ResolvedWebEndpoint definition,
                                 RequestMappingInfo mapping,
                                 Object handler,
                                 Method handlerMethod) {
        this(definition, mapping, handler, handlerMethod, null);
    }
}
