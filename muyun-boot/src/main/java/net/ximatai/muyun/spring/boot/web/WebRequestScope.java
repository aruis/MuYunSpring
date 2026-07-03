package net.ximatai.muyun.spring.boot.web;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

import java.util.Map;
import java.util.Objects;

public record WebRequestScope(Map<String, String> pathVariables) {
    public WebRequestScope {
        pathVariables = pathVariables == null ? Map.of() : Map.copyOf(pathVariables);
    }

    public static WebRequestScope empty() {
        return new WebRequestScope(Map.of());
    }

    public static WebRequestScope from(UriInfo uriInfo) {
        if (uriInfo == null) {
            return empty();
        }
        MultivaluedMap<String, String> parameters = uriInfo.getPathParameters();
        if (parameters == null || parameters.isEmpty()) {
            return empty();
        }
        return new WebRequestScope(parameters.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getFirst()
                )));
    }

    public String pathVariable(String key) {
        return pathVariables.get(Objects.requireNonNull(key, "key must not be null"));
    }
}
