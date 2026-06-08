package net.ximatai.muyun.spring.platform.workflow;

import java.time.Instant;

public record WorkflowRuntimeAddSignExplanationView(
        String originType,
        String dimension,
        Boolean isAddSignRoute,
        String nodeInstanceId,
        String nodeKey,
        WorkflowNodeType nodeType,
        WorkflowNodeStatus nodeStatus,
        String routeId,
        String routeKey,
        String routeSourceNodeKey,
        String routeTargetNodeKey,
        WorkflowRouteStatus routeStatus,
        String addSignSourceNodeKey,
        String addSignSourceNodeName,
        String addSignOperatorId,
        Instant addSignAt
) {
    public static final String ORIGIN_TYPE_ADD_SIGN = "ADD_SIGN";
    public static final String DIMENSION_NODE = "NODE";
    public static final String DIMENSION_ROUTE = "ROUTE";
}
