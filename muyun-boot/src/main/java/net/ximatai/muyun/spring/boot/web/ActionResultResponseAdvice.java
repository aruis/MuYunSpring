package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.action.CommittedChangeSet;
import net.ximatai.muyun.spring.ability.action.DataChangeModuleAliasResolver;
import net.ximatai.muyun.spring.ability.action.MutationContext;
import net.ximatai.muyun.spring.ability.action.MutationContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Map;

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
        reportAnnotatedMutationResult(returnType, request);
        CommittedChangeSet changeSet = context.committedChangeSet(moduleAliasResolver);
        return new ActionResultResponse(
                body,
                context.message(),
                changeSet.changeSetId(),
                changeSet.changes()
        );
    }

    private void reportAnnotatedMutationResult(MethodParameter returnType, ServerHttpRequest request) {
        BusinessMutationResult result = AnnotatedElementUtils.findMergedAnnotation(
                returnType.getMethod(), BusinessMutationResult.class);
        if (result == null) {
            return;
        }
        String moduleAlias = moduleAliasResolver.moduleAlias(result.module());
        switch (result.change()) {
            case UPDATED -> BusinessMutationResultSupport.successUpdated(result.code(), result.message(),
                    moduleAlias, pathVariable(request, result.recordIdPathVariable()));
            case COLLECTION_CHANGED -> BusinessMutationResultSupport.successCollectionChanged(
                    result.code(), result.message(), moduleAlias);
        }
    }

    private String pathVariable(ServerHttpRequest request, String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("recordIdPathVariable must not be blank");
        }
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            throw new IllegalArgumentException("path variables require servlet request");
        }
        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        Object variables = httpRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(variables instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("path variables are not available");
        }
        Object value = map.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("path variable is required: " + key);
        }
        return value.toString();
    }
}
