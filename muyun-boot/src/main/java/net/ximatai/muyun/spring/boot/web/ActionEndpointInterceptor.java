package net.ximatai.muyun.spring.boot.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

public class ActionEndpointInterceptor implements HandlerInterceptor {
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
        policyService.requireAuthorized(context.get());
        return true;
    }
}
