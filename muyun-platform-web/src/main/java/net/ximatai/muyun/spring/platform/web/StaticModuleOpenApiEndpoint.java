package net.ximatai.muyun.spring.platform.web;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.common.openapi.OpenApi31Projector;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/** Shared handler for exact OpenAPI mappings declared by {@link StaticModuleOpenApi}. */
@Component
public class StaticModuleOpenApiEndpoint {
    private static final String OPEN_API_SUFFIX = "/openapi";
    private final StaticModuleOpenApiGenerator generator;

    public StaticModuleOpenApiEndpoint(StaticModuleOpenApiGenerator generator) {
        this.generator = generator;
    }

    @ActionEndpoint(PlatformAction.VIEW)
    @ResponseBody
    public Map<String, Object> openApi(HttpServletRequest request) {
        String requestPath = request.getRequestURI().substring(request.getContextPath().length());
        if (!requestPath.startsWith("/") || !requestPath.endsWith(OPEN_API_SUFFIX)) {
            throw new IllegalStateException("static module OpenAPI mapping must end with /openapi: " + requestPath);
        }
        String moduleAlias = requestPath.substring(1, requestPath.length() - OPEN_API_SUFFIX.length());
        return OpenApi31Projector.project(generator.generate(moduleAlias));
    }
}
