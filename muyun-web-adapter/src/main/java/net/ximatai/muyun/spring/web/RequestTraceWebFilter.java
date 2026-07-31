package net.ximatai.muyun.spring.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.ximatai.muyun.spring.common.web.RequestTraceContext;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class RequestTraceWebFilter extends OncePerRequestFilter implements Ordered {
    public static final String MDC_TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = traceIdOf(request);
        try (RequestTraceContext.Scope ignored = RequestTraceContext.use(traceId)) {
            MDC.put(MDC_TRACE_ID, RequestTraceContext.ensureTraceId());
            response.setHeader(RequestTraceContext.TRACE_ID_HEADER, RequestTraceContext.ensureTraceId());
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
            RequestTraceContext.clear();
        }
    }

    private String traceIdOf(HttpServletRequest request) {
        String traceId = request.getHeader(RequestTraceContext.TRACE_ID_HEADER);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return request.getHeader("X-Trace-Id");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
