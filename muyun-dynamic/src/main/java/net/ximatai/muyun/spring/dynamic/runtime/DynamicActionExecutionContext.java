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
        DynamicActionAvailability availability
) {
}
