package net.ximatai.muyun.spring.boot.web;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.identity.CurrentUserProvider;
import net.ximatai.muyun.spring.common.tenant.TenantContext;

import java.util.Optional;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class CurrentUserWebFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final String CURRENT_USER_SCOPE = CurrentUserWebFilter.class.getName() + ".CURRENT_USER_SCOPE";
    private static final String TENANT_SCOPE = CurrentUserWebFilter.class.getName() + ".TENANT_SCOPE";

    private final CurrentUserProvider currentUserProvider;

    public CurrentUserWebFilter(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        try (BearerTokenCurrentUserProvider.Scope ignored = BearerTokenCurrentUserProvider.useAuthorizationHeader(
                requestContext.getHeaderString("Authorization"))) {
            Optional<CurrentUser> currentUser = currentUserProvider.currentUser();
            if (currentUser.isEmpty()) {
                return;
            }
            requestContext.setProperty(CURRENT_USER_SCOPE, CurrentUserContext.use(currentUser.get()));
            TenantContext.Scope tenantScope = tenantScope(currentUser.get());
            if (tenantScope != null) {
                requestContext.setProperty(TENANT_SCOPE, tenantScope);
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        close(requestContext.getProperty(TENANT_SCOPE));
        close(requestContext.getProperty(CURRENT_USER_SCOPE));
    }

    private TenantContext.Scope tenantScope(CurrentUser currentUser) {
        if (currentUser.system()) {
            return TenantContext.system("system user web request");
        }
        String tenantId = currentUser.tenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        return TenantContext.use(tenantId);
    }

    private void close(Object scope) {
        if (scope instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }
}
