package net.ximatai.muyun.spring.boot.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import java.util.Optional;

public class ActionEndpointInterceptor implements AsyncHandlerInterceptor {
    private static final String ACTION_CONTEXT_SCOPE_ATTRIBUTE =
            ActionEndpointInterceptor.class.getName() + ".ACTION_CONTEXT_SCOPE";

    private final ActionExecutionPolicyService policyService;
    private final ActionEndpointContextResolver contextResolver;

    public ActionEndpointInterceptor(ActionExecutionPolicyService policyService,
                                     ActionEndpointContextResolver contextResolver) {
        this.policyService = policyService;
        this.contextResolver = contextResolver;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        ActionEndpoint endpoint = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), ActionEndpoint.class);
        if (endpoint == null) {
            return true;
        }
        Optional<ActionExecutionContext> context = contextResolver.resolve(request, handlerMethod, endpoint);
        if (context.isEmpty()) {
            throw new IllegalStateException("@ActionEndpoint requires module alias: "
                    + handlerMethod.getBeanType().getName() + "#" + handlerMethod.getMethod().getName());
        }
        ActionExecutionContext resolved = context.get();
        policyService.requireAuthorized(resolved);
        request.setAttribute(ACTION_CONTEXT_SCOPE_ATTRIBUTE, ActionExecutionContextHolder.use(resolved));
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        closeActionContext(request);
    }

    @Override
    public void afterConcurrentHandlingStarted(@NonNull HttpServletRequest request,
                                               @NonNull HttpServletResponse response,
                                               @NonNull Object handler) {
        closeActionContext(request);
    }

    private void closeActionContext(HttpServletRequest request) {
        Object scope = request.getAttribute(ACTION_CONTEXT_SCOPE_ATTRIBUTE);
        request.removeAttribute(ACTION_CONTEXT_SCOPE_ATTRIBUTE);
        if (scope instanceof ActionExecutionContextHolder.Scope contextScope) {
            contextScope.close();
        }
    }
}
