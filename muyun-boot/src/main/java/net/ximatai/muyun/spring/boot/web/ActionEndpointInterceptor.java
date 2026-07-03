package net.ximatai.muyun.spring.boot.web;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import net.ximatai.muyun.spring.common.di.ObjectProvider;
import net.ximatai.muyun.spring.common.identity.ActingContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionAuthorizationResult;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import net.ximatai.muyun.spring.iam.employee.EmployeeDelegationService;

@Provider
@Priority(Priorities.AUTHORIZATION)
@ApplicationScoped
public class ActionEndpointInterceptor implements ContainerRequestFilter, ContainerResponseFilter {
    static final String ACTION_CONTEXT_PROPERTY = ActionEndpointInterceptor.class.getName() + ".ACTION_CONTEXT";
    private static final String ACTION_SCOPE_PROPERTY = ActionEndpointInterceptor.class.getName() + ".ACTION_SCOPE";
    private static final String ACTING_SCOPE_PROPERTY = ActionEndpointInterceptor.class.getName() + ".ACTING_SCOPE";

    private final ActionExecutionPolicyService policyService;
    private final ActionEndpointContextResolver contextResolver;
    private final ActingRequestResolver actingRequestResolver;
    @Context
    ResourceInfo resourceInfo;

    @Inject
    public ActionEndpointInterceptor(ActionExecutionPolicyService policyService,
                                     ActionEndpointContextResolver contextResolver,
                                     ObjectProvider<EmployeeDelegationService> employeeDelegationService) {
        this(policyService, contextResolver, actingRequestResolver(employeeDelegationService));
    }

    public ActionEndpointInterceptor(ActionExecutionPolicyService policyService,
                                     ActionEndpointContextResolver contextResolver) {
        this(policyService, contextResolver, (ActingRequestResolver) null);
    }

    public ActionEndpointInterceptor(ActionExecutionPolicyService policyService,
                                     ActionEndpointContextResolver contextResolver,
                                     ActingRequestResolver actingRequestResolver) {
        this.policyService = policyService;
        this.contextResolver = contextResolver;
        this.actingRequestResolver = actingRequestResolver;
    }

    private static ActingRequestResolver actingRequestResolver(ObjectProvider<EmployeeDelegationService> provider) {
        EmployeeDelegationService delegationService = provider == null ? null : provider.getIfAvailable();
        return delegationService == null ? null : new ActingRequestResolver(delegationService);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        ActionExecutionContext context = actionContext(requestContext);
        if (context == null) {
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
            if (actingRequestResolver != null && ActingRequestResolver.hasActingRequest(requestContext)) {
                requestContext.setProperty(ACTING_SCOPE_PROPERTY, ActingContextHolder.use(
                        actingRequestResolver.resolve(requestContext, authorized)
                                .orElseThrow(() -> new IllegalStateException("acting context is not resolved"))
                ));
            }
        } catch (RuntimeException exception) {
            scope.close();
            throw exception;
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object actingScope = requestContext.getProperty(ACTING_SCOPE_PROPERTY);
        if (actingScope instanceof ActingContextHolder.Scope scope) {
            scope.close();
        }
        Object scope = requestContext.getProperty(ACTION_SCOPE_PROPERTY);
        if (scope instanceof ActionExecutionContextHolder.Scope actionScope) {
            actionScope.close();
        }
    }

    private ActionExecutionContext actionContext(ContainerRequestContext requestContext) {
        Object value = requestContext.getProperty(ACTION_CONTEXT_PROPERTY);
        if (value instanceof ActionExecutionContext context) {
            return context;
        }
        return contextResolver.resolve(requestContext, resourceInfo)
                .map(context -> {
                    requestContext.setProperty(ACTION_CONTEXT_PROPERTY, context);
                    return context;
                })
                .orElse(null);
    }
}
