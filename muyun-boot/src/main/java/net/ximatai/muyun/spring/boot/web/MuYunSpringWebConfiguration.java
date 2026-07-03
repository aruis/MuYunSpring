package net.ximatai.muyun.spring.boot.web;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@ApplicationScoped
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class MuYunSpringWebConfiguration implements ContainerRequestFilter, ContainerResponseFilter {
    static final String ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    static final String ALLOW_METHODS = "Access-Control-Allow-Methods";
    static final String ALLOW_HEADERS = "Access-Control-Allow-Headers";
    static final String EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    static final String MAX_AGE = "Access-Control-Max-Age";
    static final String VARY = "Vary";
    private static final String ORIGIN = "Origin";
    private static final String REQUEST_METHOD = "Access-Control-Request-Method";
    private static final String REQUEST_HEADERS = "Access-Control-Request-Headers";
    private static final String METHODS = "GET,POST,PUT,PATCH,DELETE,OPTIONS";
    private static final String EXPOSED = "Authorization";
    private static final String DEFAULT_ALLOWED_HEADERS = "*";
    private static final String DEFAULT_MAX_AGE = "3600";

    private final MuYunSpringCorsProperties corsProperties;

    public MuYunSpringWebConfiguration(MuYunSpringCorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String origin = requestContext.getHeaderString(ORIGIN);
        if (!isAllowedOrigin(origin) || !HttpMethod.OPTIONS.equalsIgnoreCase(requestContext.getMethod())
                || requestContext.getHeaderString(REQUEST_METHOD) == null) {
            return;
        }
        requestContext.abortWith(cors(Response.noContent(), origin,
                allowedHeaders(requestContext.getHeaderString(REQUEST_HEADERS))).build());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String origin = requestContext.getHeaderString(ORIGIN);
        if (!isAllowedOrigin(origin)) {
            return;
        }
        responseContext.getHeaders().putSingle(ALLOW_ORIGIN, origin);
        responseContext.getHeaders().putSingle(ALLOW_METHODS, METHODS);
        responseContext.getHeaders().putSingle(ALLOW_HEADERS,
                allowedHeaders(requestContext.getHeaderString(REQUEST_HEADERS)));
        responseContext.getHeaders().putSingle(EXPOSE_HEADERS, EXPOSED);
        responseContext.getHeaders().putSingle(MAX_AGE, DEFAULT_MAX_AGE);
        responseContext.getHeaders().putSingle(VARY, ORIGIN);
    }

    private Response.ResponseBuilder cors(Response.ResponseBuilder response, String origin, String allowedHeaders) {
        return response
                .header(ALLOW_ORIGIN, origin)
                .header(ALLOW_METHODS, METHODS)
                .header(ALLOW_HEADERS, allowedHeaders)
                .header(EXPOSE_HEADERS, EXPOSED)
                .header(MAX_AGE, DEFAULT_MAX_AGE)
                .header(VARY, ORIGIN);
    }

    private boolean isAllowedOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return false;
        }
        List<String> allowedOrigins = corsProperties.getAllowedOrigins();
        return allowedOrigins != null && allowedOrigins.stream().anyMatch(origin::equals);
    }

    private String allowedHeaders(String requestedHeaders) {
        if (requestedHeaders == null || requestedHeaders.isBlank()) {
            return DEFAULT_ALLOWED_HEADERS;
        }
        return requestedHeaders;
    }
}
