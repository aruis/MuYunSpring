package net.ximatai.muyun.spring.boot.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.ximatai.muyun.spring.ability.action.MutationContext;
import net.ximatai.muyun.spring.ability.action.MutationContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

public class BusinessMutationInterceptor implements AsyncHandlerInterceptor {
    private static final String MUTATION_CONTEXT_SCOPE_ATTRIBUTE =
            BusinessMutationInterceptor.class.getName() + ".MUTATION_CONTEXT_SCOPE";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod) || !isBusinessMutation(handlerMethod)) {
            return true;
        }
        request.setAttribute(MUTATION_CONTEXT_SCOPE_ATTRIBUTE,
                MutationContextHolder.use(new MutationContext()));
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        closeMutationContext(request);
    }

    @Override
    public void afterConcurrentHandlingStarted(@NonNull HttpServletRequest request,
                                               @NonNull HttpServletResponse response,
                                               @NonNull Object handler) {
        closeMutationContext(request);
    }

    private boolean isBusinessMutation(HandlerMethod handlerMethod) {
        return WebAnnotationSupport.hasMergedMethodOrTypeAnnotation(handlerMethod.getMethod(),
                handlerMethod.getBeanType(), BusinessMutation.class)
                && ActionExecutionContextHolder.current().isPresent();
    }

    private void closeMutationContext(HttpServletRequest request) {
        Object scope = request.getAttribute(MUTATION_CONTEXT_SCOPE_ATTRIBUTE);
        request.removeAttribute(MUTATION_CONTEXT_SCOPE_ATTRIBUTE);
        if (scope instanceof MutationContextHolder.Scope contextScope) {
            contextScope.close();
        }
    }
}
