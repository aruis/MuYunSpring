package net.ximatai.muyun.spring.platform.workflow;

import jakarta.enterprise.context.Dependent;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Dependent
public class WorkflowRouteInstanceStateService {
    public void applyActivation(List<WorkflowRouteInstance> routes, WorkflowActivationResult activation,
                                String operatorId, Instant operatedAt) {
        if (routes == null || activation == null) {
            return;
        }
        Set<String> traversedRouteKeys = new HashSet<>(activation.traversedRouteKeys());
        Instant now = operatedAt == null ? Instant.now() : operatedAt;
        for (WorkflowRouteInstance route : routes) {
            if (route == null || !traversedRouteKeys.contains(route.getRouteKey())) {
                continue;
            }
            route.setRouteStatus(WorkflowRouteStatus.EFFECTIVE);
            route.setSelectedBy(operatorId);
            route.setSelectedAt(now);
        }
    }
}
