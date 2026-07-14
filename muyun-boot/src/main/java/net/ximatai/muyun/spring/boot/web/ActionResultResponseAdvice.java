package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.action.CommittedChangeSet;
import net.ximatai.muyun.spring.ability.action.DataChangeModuleAliasResolver;
import net.ximatai.muyun.spring.ability.action.MutationContext;
import net.ximatai.muyun.spring.ability.action.MutationContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class ActionResultResponseAdvice implements ResponseBodyAdvice<Object> {
    private final DataChangeModuleAliasResolver moduleAliasResolver;

    public ActionResultResponseAdvice(DataChangeModuleAliasResolver moduleAliasResolver) {
        this.moduleAliasResolver = moduleAliasResolver;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        if (returnType == null || returnType.getMethod() == null) {
            return false;
        }
        if (MutationContextHolder.current().isEmpty()) {
            return false;
        }
        return AnnotatedElementUtils.hasAnnotation(returnType.getMethod(), BusinessMutation.class)
                || AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), BusinessMutation.class);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body instanceof ActionResultResponse || body instanceof PlatformWebError) {
            return body;
        }
        MutationContext context = MutationContextHolder.current().orElse(null);
        if (context == null) {
            return body;
        }
        CommittedChangeSet changeSet = context.committedChangeSet(moduleAliasResolver);
        return new ActionResultResponse(
                actionData(body),
                context.message(),
                changeSet.changeSetId(),
                changeSet.changes()
        );
    }

    private Object actionData(Object body) {
        if (body instanceof WebRecordResponse<?> recordResponse) {
            return recordResponse.record();
        }
        if (body instanceof WebCountResponse) {
            return null;
        }
        return body;
    }
}
