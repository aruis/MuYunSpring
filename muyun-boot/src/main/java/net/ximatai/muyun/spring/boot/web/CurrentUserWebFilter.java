package net.ximatai.muyun.spring.boot.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.identity.CurrentUserProvider;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

public class CurrentUserWebFilter extends OncePerRequestFilter {
    private final CurrentUserProvider currentUserProvider;

    public CurrentUserWebFilter(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Optional<CurrentUser> currentUser = currentUserProvider.currentUser();
        if (currentUser.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        try (CurrentUserContext.Scope ignored = CurrentUserContext.use(currentUser.get())) {
            doFilterWithTenantScope(currentUser.get(), request, response, filterChain);
        }
    }

    private void doFilterWithTenantScope(CurrentUser currentUser,
                                         HttpServletRequest request,
                                         HttpServletResponse response,
                                         FilterChain filterChain) throws ServletException, IOException {
        if (currentUser.system()) {
            try (TenantContext.Scope ignored = TenantContext.system()) {
                filterChain.doFilter(request, response);
            }
            return;
        }
        String tenantId = currentUser.tenantId();
        if (tenantId == null || tenantId.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        try (TenantContext.Scope ignored = TenantContext.use(tenantId)) {
            filterChain.doFilter(request, response);
        }
    }
}
