package net.ximatai.muyun.spring.boot.web.endpoint;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Authoritative catalog of mappings that were actually accepted by Spring MVC. */
public class RegisteredWebEndpointCatalog {
    private final Map<String, RegisteredWebEndpoint> byEndpointId = new LinkedHashMap<>();

    public synchronized void register(RegisteredWebEndpoint endpoint) {
        String endpointId = endpoint.definition().endpointId();
        RegisteredWebEndpoint existing = byEndpointId.putIfAbsent(endpointId, endpoint);
        if (existing != null) {
            throw new IllegalStateException("duplicate registered endpoint: " + endpointId);
        }
    }

    public synchronized List<RegisteredWebEndpoint> endpoints() {
        return List.copyOf(byEndpointId.values());
    }

    public synchronized boolean contains(RequestMappingInfo mapping, HandlerMethod handlerMethod) {
        if (mapping == null || handlerMethod == null) {
            return false;
        }
        return byEndpointId.values().stream()
                .anyMatch(endpoint -> endpoint.mapping().equals(mapping)
                        && (endpoint.handler() == handlerMethod.getBean()
                        || Objects.equals(endpoint.handler(), handlerMethod.getBean()))
                        && endpoint.handlerMethod().equals(handlerMethod.getMethod()));
    }

    public synchronized Optional<RegisteredWebEndpoint> find(HandlerMethod handlerMethod) {
        if (handlerMethod == null) {
            return Optional.empty();
        }
        Object bean = handlerMethod.getBean();
        Method method = handlerMethod.getMethod();
        List<RegisteredWebEndpoint> matches = byEndpointId.values().stream()
                .filter(endpoint -> (endpoint.handler() == bean || Objects.equals(endpoint.handler(), bean))
                        && endpoint.handlerMethod().equals(method))
                .toList();
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }

    public synchronized Optional<RegisteredWebEndpoint> find(HttpServletRequest request,
                                                              HandlerMethod handlerMethod) {
        if (request == null || handlerMethod == null) {
            return Optional.empty();
        }
        String pattern = String.valueOf(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
        String requestMethod = request.getMethod();
        if (request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) == null) {
            return find(handlerMethod);
        }
        return byEndpointId.values().stream()
                .filter(endpoint -> endpoint.handler() == handlerMethod.getBean()
                        || Objects.equals(endpoint.handler(), handlerMethod.getBean()))
                .filter(endpoint -> endpoint.handlerMethod().equals(handlerMethod.getMethod()))
                .filter(endpoint -> endpoint.definition().path().equals(pattern))
                .filter(endpoint -> endpoint.definition().method().name().equals(requestMethod))
                .findFirst();
    }

    public synchronized RegisteredWebEndpoint require(HttpServletRequest request, Object handler, Method method) {
        String pattern = String.valueOf(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
        return byEndpointId.values().stream()
                .filter(endpoint -> endpoint.handler() == handler || Objects.equals(endpoint.handler(), handler))
                .filter(endpoint -> endpoint.handlerMethod().equals(method))
                .filter(endpoint -> endpoint.definition().path().equals(pattern))
                .filter(endpoint -> endpoint.definition().method().name().equals(request.getMethod()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("registered platform endpoint not found: "
                        + request.getMethod() + " " + pattern));
    }
}
