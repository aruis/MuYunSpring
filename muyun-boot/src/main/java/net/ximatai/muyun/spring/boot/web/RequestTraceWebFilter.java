package net.ximatai.muyun.spring.boot.web;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import net.ximatai.muyun.spring.boot.dynamic.DynamicWebRequest;
import net.ximatai.muyun.spring.common.web.RequestTraceContext;
import org.slf4j.MDC;

@Provider
@Priority(Priorities.AUTHENTICATION - 100)
public class RequestTraceWebFilter implements ContainerRequestFilter, ContainerResponseFilter {
    public static final String MDC_TRACE_ID = "traceId";
    private static final String TRACE_SCOPE = RequestTraceWebFilter.class.getName() + ".TRACE_SCOPE";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        RequestTraceContext.Scope scope = RequestTraceContext.use(traceIdOf(requestContext));
        requestContext.setProperty(TRACE_SCOPE, scope);
        DynamicWebRequest.useRequestPath("/" + requestContext.getUriInfo().getPath(false));
        MDC.put(MDC_TRACE_ID, RequestTraceContext.ensureTraceId());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        responseContext.getHeaders().putSingle(RequestTraceContext.TRACE_ID_HEADER, RequestTraceContext.ensureTraceId());
        Object scope = requestContext.getProperty(TRACE_SCOPE);
        if (scope instanceof RequestTraceContext.Scope traceScope) {
            traceScope.close();
        }
        MDC.remove(MDC_TRACE_ID);
        DynamicWebRequest.clearRequestPath();
        RequestTraceContext.clear();
    }

    private String traceIdOf(ContainerRequestContext requestContext) {
        String traceId = requestContext.getHeaderString(RequestTraceContext.TRACE_ID_HEADER);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return requestContext.getHeaderString("X-Trace-Id");
    }
}
