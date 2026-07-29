package net.ximatai.muyun.spring.boot.web.endpoint;

import net.ximatai.muyun.spring.ability.PlatformOperationDefinition;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import org.springframework.web.bind.annotation.RequestMethod;

/** HTTP projection compiled from one semantic platform operation. */
public record WebEndpointProjection(PlatformOperationDefinition operation,
                                    String moduleAlias,
                                    String abilityCode,
                                    ActionExecutionPolicy executionPolicy,
                                    RequestMethod method,
                                    String endpointId,
                                    String path) {
    ResolvedWebEndpoint resolve() {
        return new ResolvedWebEndpoint(endpointId, moduleAlias, abilityCode, operation.operationCode(),
                operation.action(), method, path, ResolvedWebEndpoint.Source.STATIC_ABILITY, executionPolicy);
    }
}
