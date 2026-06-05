package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class WorkflowRuntimeActivationService {
    public WorkflowActivationResult activate(WorkflowActivationRequest request) {
        if (request == null || request.graph() == null) {
            throw new PlatformException("workflow activation request and graph must not be null");
        }
        ArrayDeque<WorkflowActivationTarget> queue = new ArrayDeque<>(request.targets());
        Set<String> visited = new HashSet<>();
        List<String> activated = new ArrayList<>();
        List<String> routes = new ArrayList<>();
        List<String> approvalBlocks = new ArrayList<>();
        List<String> taskBlocks = new ArrayList<>();
        List<String> convergeWaits = new ArrayList<>();
        List<WorkflowMilestoneType> milestones = new ArrayList<>();
        boolean completed = false;
        int steps = 0;

        while (!queue.isEmpty()) {
            if (++steps > request.maxSteps()) {
                throw new PlatformException("workflow activation exceeded max steps: " + request.maxSteps());
            }
            WorkflowActivationTarget target = queue.removeFirst();
            String nodeKey = target.nodeKey();
            if (!visited.add(visitedKey(target))) {
                continue;
            }
            WorkflowNodeDefinition node = request.graph().requireNode(nodeKey);
            activated.add(nodeKey);

            if (node.getNodeType() == WorkflowNodeType.APPROVAL) {
                if (node.getApprovalMode() == WorkflowApprovalMode.NOTICE) {
                    enqueueOutgoing(request, nodeKey, queue, routes);
                } else {
                    approvalBlocks.add(nodeKey);
                }
                continue;
            }
            if (node.getNodeType() == WorkflowNodeType.TASK) {
                taskBlocks.add(nodeKey);
                continue;
            }
            if (node.getNodeType() == WorkflowNodeType.BRANCH) {
                enqueueSelectedBranchRoutes(request, nodeKey, queue, routes);
                continue;
            }
            if (node.getNodeType() == WorkflowNodeType.CONVERGE
                    && !request.passedConvergeNodeKeys().contains(nodeKey)) {
                convergeWaits.add(nodeKey);
                continue;
            }
            if (node.getNodeType() == WorkflowNodeType.MILESTONE && node.getMilestoneType() != null) {
                milestones.add(node.getMilestoneType());
            }
            if (node.getNodeType() == WorkflowNodeType.END) {
                completed = true;
                continue;
            }
            enqueueOutgoing(request, nodeKey, queue, routes);
        }
        return new WorkflowActivationResult(activated, routes, approvalBlocks, taskBlocks, convergeWaits,
                milestones, completed);
    }

    private void enqueueSelectedBranchRoutes(WorkflowActivationRequest request, String nodeKey,
                                             ArrayDeque<WorkflowActivationTarget> queue, List<String> traversedRouteKeys) {
        List<WorkflowLinkDefinition> outgoing = request.graph().outgoing(nodeKey);
        Set<String> selected = request.selectedRouteKeysByBranch().get(nodeKey);
        List<WorkflowLinkDefinition> matched = selected == null || selected.isEmpty()
                ? defaultRoutes(outgoing)
                : outgoing.stream().filter(link -> selected.contains(link.getRouteKey())).toList();
        if (matched.isEmpty()) {
            throw new PlatformException("workflow branch has no selected route: " + nodeKey);
        }
        enqueueLinks(matched, queue, traversedRouteKeys);
    }

    private List<WorkflowLinkDefinition> defaultRoutes(List<WorkflowLinkDefinition> outgoing) {
        List<WorkflowLinkDefinition> defaults = outgoing.stream()
                .filter(link -> Boolean.TRUE.equals(link.getDefaultRoute()))
                .toList();
        return defaults.isEmpty() ? outgoing : defaults;
    }

    private void enqueueOutgoing(WorkflowActivationRequest request, String nodeKey,
                                 ArrayDeque<WorkflowActivationTarget> queue, List<String> traversedRouteKeys) {
        enqueueLinks(request.graph().outgoing(nodeKey), queue, traversedRouteKeys);
    }

    private void enqueueLinks(List<WorkflowLinkDefinition> links, ArrayDeque<WorkflowActivationTarget> queue,
                              List<String> traversedRouteKeys) {
        Set<String> routeKeys = new LinkedHashSet<>();
        for (WorkflowLinkDefinition link : links) {
            queue.addLast(new WorkflowActivationTarget(link.getTargetNodeKey(), link.getRouteKey()));
            if (link.getRouteKey() != null && routeKeys.add(link.getRouteKey())) {
                traversedRouteKeys.add(link.getRouteKey());
            }
        }
    }

    private String visitedKey(WorkflowActivationTarget target) {
        return target.nodeKey() + "@" + (target.routeInstanceId() == null ? "" : target.routeInstanceId());
    }
}
