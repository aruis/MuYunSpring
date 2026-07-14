package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.action.CommittedChangeSet;
import net.ximatai.muyun.spring.ability.action.DataChangeModuleAliasResolver;
import net.ximatai.muyun.spring.ability.action.MutationContext;
import net.ximatai.muyun.spring.ability.action.MutationContextHolder;
import net.ximatai.muyun.spring.common.model.contract.Identifiable;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
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
        Object data = actionData(body);
        applyStandardMutation(returnType, request, body, data);
        CommittedChangeSet changeSet = context.committedChangeSet(moduleAliasResolver);
        return new ActionResultResponse(
                data,
                context.message(),
                changeSet.changeSetId(),
                changeSet.changes()
        );
    }

    private Object actionData(Object body) {
        if (body instanceof WebRecordResponse<?> recordResponse) {
            return recordResponse.record();
        }
        if (body instanceof WebCountResponse countResponse) {
            return new CountData(countResponse.count());
        }
        return body;
    }

    private record CountData(int count) {
    }

    private void applyStandardMutation(MethodParameter returnType,
                                       ServerHttpRequest request,
                                       Object body,
                                       Object data) {
        StandardMutation standardMutation = standardMutation(returnType);
        if (standardMutation == null || !mutationAffectedRecord(body)) {
            return;
        }
        String moduleAlias = ActionExecutionContextHolder.current()
                .map(context -> context.moduleAlias())
                .orElse(null);
        String recordId = recordId(data, request);
        if (moduleAlias == null || moduleAlias.isBlank() || recordId == null || recordId.isBlank()) {
            return;
        }
        StaticCrudActionResultSupport.report(standardMutation.value(), moduleAlias, recordId);
    }

    private StandardMutation standardMutation(MethodParameter returnType) {
        StandardMutation method = AnnotatedElementUtils.findMergedAnnotation(
                returnType.getMethod(), StandardMutation.class);
        if (method != null) {
            return method;
        }
        return AnnotatedElementUtils.findMergedAnnotation(returnType.getContainingClass(), StandardMutation.class);
    }

    private boolean mutationAffectedRecord(Object body) {
        return !(body instanceof WebCountResponse countResponse) || countResponse.count() > 0;
    }

    private String recordId(Object data, ServerHttpRequest request) {
        if (data instanceof Identifiable identifiable && identifiable.getId() != null
                && !identifiable.getId().isBlank()) {
            return identifiable.getId().trim();
        }
        if (request instanceof ServletServerHttpRequest servletRequest) {
            Object value = servletRequest.getServletRequest()
                    .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (value instanceof Map<?, ?> variables) {
                Object id = variables.get("id");
                if (id instanceof String text && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return null;
    }
}
