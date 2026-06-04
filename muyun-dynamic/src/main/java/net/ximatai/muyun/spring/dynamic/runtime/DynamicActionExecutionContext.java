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
        String systemReason,
        String operatorId,
        String operatorType,
        String authorizationDecision,
        String authorizationPermissionCode,
        String authorizationPermissionActionCode,
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
                                         String systemReason,
                                         DynamicActionAvailability availability) {
        this(moduleAlias, entityAlias, actionCode, action, recordId, traceId, tenantId, systemContext,
                systemReason, null, null, null, null, null, availability);
    }

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
                null, null, null, null, null, null, availability);
    }
}
