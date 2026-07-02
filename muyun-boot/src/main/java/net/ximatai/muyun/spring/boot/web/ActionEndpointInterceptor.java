package net.ximatai.muyun.spring.boot.web;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import net.ximatai.muyun.spring.common.platform.ActionAuthorizationResult;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class ActionEndpointInterceptor implements ContainerRequestFilter, ContainerResponseFilter {
    static final String ACTION_CONTEXT_PROPERTY = ActionEndpointInterceptor.class.getName() + ".ACTION_CONTEXT";
    private static final String ACTION_SCOPE_PROPERTY = ActionEndpointInterceptor.class.getName() + ".ACTION_SCOPE";

    private final ActionExecutionPolicyService policyService;
    private final ActionEndpointContextResolver contextResolver;
    private final ActingRequestResolver actingRequestResolver;

    public ActionEndpointInterceptor(ActionExecutionPolicyService policyService,
                                     ActionEndpointContextResolver contextResolver) {
        this(policyService, contextResolver, null);
    }

    public ActionEndpointInterceptor(ActionExecutionPolicyService policyService,
                                     ActionEndpointContextResolver contextResolver,
                                     ActingRequestResolver actingRequestResolver) {
        this.policyService = policyService;
        this.contextResolver = contextResolver;
        this.actingRequestResolver = actingRequestResolver;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Object value = requestContext.getProperty(ACTION_CONTEXT_PROPERTY);
        if (!(value instanceof ActionExecutionContext context)) {
            return;
        }
        ActionExecutionContextHolder.Scope scope = ActionExecutionContextHolder.use(context);
        try {
            ActionAuthorizationResult result = policyService.authorize(context);
            ActionExecutionContext authorized = context.withAuthorizationResult(result);
            scope.close();
            scope = ActionExecutionContextHolder.use(authorized);
            requestContext.setProperty(ACTION_CONTEXT_PROPERTY, authorized);
            requestContext.setProperty(ACTION_SCOPE_PROPERTY, scope);
        } catch (RuntimeException exception) {
            scope.close();
            throw exception;
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object scope = requestContext.getProperty(ACTION_SCOPE_PROPERTY);
        if (scope instanceof ActionExecutionContextHolder.Scope actionScope) {
            actionScope.close();
        }
    }
}
