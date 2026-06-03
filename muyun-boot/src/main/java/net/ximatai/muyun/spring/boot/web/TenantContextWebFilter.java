package net.ximatai.muyun.spring.boot.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantContextWebFilter extends OncePerRequestFilter {
    public static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId == null || tenantId.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        try (TenantContext.Scope ignored = TenantContext.use(tenantId)) {
            filterChain.doFilter(request, response);
        }
    }
}
