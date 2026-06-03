package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;

public record DynamicActionExecutionContext(
        String moduleAlias,
        String entityAlias,
        String actionCode,
        DynamicActionDescriptor action,
        String recordId,
        String traceId,
        String tenantId,
        boolean systemContext,
        String operatorId,
        String operatorType,
        String authorizationDecision,
        DynamicActionAvailability availability
) {
    public DynamicActionExecutionContext(String moduleAlias,
                                         String entityAlias,
                                         String actionCode,
                                         DynamicActionDescriptor action,
                                         String recordId,
                                         String traceId,
                                         String tenantId,
                                         boolean systemContext,
                                         DynamicActionAvailability availability) {
        this(moduleAlias, entityAlias, actionCode, action, recordId, traceId, tenantId, systemContext,
                null, null, null, availability);
    }
}
