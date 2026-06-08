package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WorkflowManualRouteSelectionPolicy {
    private final WorkflowManualBranchSelectorResolver selectorResolver = new WorkflowManualBranchSelectorResolver();

    public Map<String, Set<String>> selectedRouteKeysBySubmitBranch(WorkflowRuntimeGraph graph,
                                                                    WorkflowInstance instance,
                                                                    String startNodeKey,
                                                                    String selectedRouteKey,
                                                                    String selectedReason,
                                                                    String operatorId) {
        return selectedRouteKeysBySubmitBranch(graph, instance, startNodeKey, List.of(), selectedRouteKey,
                selectedReason, operatorId);
    }

    public Map<String, Set<String>> selectedRouteKeysBySubmitBranch(WorkflowRuntimeGraph graph,
                                                                    WorkflowInstance instance,
                                                                    String startNodeKey,
                                                                    List<WorkflowManualRouteSelection> manualRouteSelections,
                                                                    String selectedRouteKey,
                                                                    String selectedReason,
                                                                    String operatorId) {
        boolean structured = hasManualRouteSelections(manualRouteSelections);
        String selectedKey = structured ? null : textOrNull(selectedRouteKey);
        Map<String, Set<String>> selectedByBranch = structured
                ? selectedSubmitRoutes(graph, startNodeKey, manualRouteSelections)
                : selectedKey == null ? Map.of() : selectedSubmitRoute(graph, startNodeKey, selectedKey);
        Set<String> branchNodeKeys = reachableSubmitBranchNodeKeys(graph, startNodeKey);
        requireManualBranches(graph, instance, List.of(), List.of(), branchNodeKeys, selectedByBranch,
                startNodeKey, selectedReasonsByBranch(manualRouteSelections, selectedRouteKey, selectedReason),
                operatorId);
        return selectedByBranch;
    }

    public void requireCompletedBranchSelection(WorkflowInstance instance,
                                                List<WorkflowNodeInstance> nodes,
                                                List<WorkflowTask> tasks,
                                                String completedNodeKey,
                                                List<WorkflowRouteInstance> selectedInitialRoutes,
                                                String selectedRouteKey,
                                                String selectedReason,
                                                String operatorId) {
        requireCompletedBranchSelection(instance, nodes, tasks, completedNodeKey, selectedInitialRoutes,
                List.of(), selectedRouteKey, selectedReason, operatorId);
    }

    public void requireCompletedBranchSelection(WorkflowInstance instance,
                                                List<WorkflowNodeInstance> nodes,
                                                List<WorkflowTask> tasks,
                                                String completedNodeKey,
                                                List<WorkflowRouteInstance> selectedInitialRoutes,
                                                List<WorkflowManualRouteSelection> manualRouteSelections,
                                                String selectedRouteKey,
                                                String selectedReason,
                                                String operatorId) {
        WorkflowNodeInstance completedNode = nodesByKey(nodes).get(completedNodeKey);
        if (completedNode == null || completedNode.getNodeType() != WorkflowNodeType.BRANCH
                || routeMode(completedNode) != WorkflowRouteMode.MANUAL) {
            return;
        }
        String selectedKey = selectedRouteKeyForBranch(completedNodeKey, manualRouteSelections, selectedRouteKey);
        if (selectedKey == null || selectedInitialRoutes.stream()
                .noneMatch(route -> selectedKey.equals(route.getRouteKey()))) {
            throw new PlatformException("workflow manual branch requires selected route: " + completedNodeKey);
        }
        requireManualSelection(instance, nodeDefinition(completedNode), nodes, tasks, completedNodeKey,
                selectedReasonForBranch(completedNodeKey, manualRouteSelections, selectedRouteKey, selectedReason),
                operatorId);
    }

    public Map<String, Set<String>> selectedRouteKeysByProgressionBranch(List<WorkflowRouteInstance> routes,
                                                                         List<WorkflowNodeInstance> nodes,
                                                                         WorkflowRuntimeGraph graph,
                                                                         WorkflowInstance instance,
                                                                         List<WorkflowTask> tasks,
                                                                         String completedNodeKey,
                                                                         List<WorkflowRouteInstance> selectedInitialRoutes,
                                                                         String selectedRouteKey,
                                                                         String selectedReason,
                                                                         String operatorId) {
        return selectedRouteKeysByProgressionBranch(routes, nodes, graph, instance, tasks, completedNodeKey,
                selectedInitialRoutes, List.of(), selectedRouteKey, selectedReason, operatorId);
    }

    public Map<String, Set<String>> selectedRouteKeysByProgressionBranch(List<WorkflowRouteInstance> routes,
                                                                         List<WorkflowNodeInstance> nodes,
                                                                         WorkflowRuntimeGraph graph,
                                                                         WorkflowInstance instance,
                                                                         List<WorkflowTask> tasks,
                                                                         String completedNodeKey,
                                                                         List<WorkflowRouteInstance> selectedInitialRoutes,
                                                                         List<WorkflowManualRouteSelection> manualRouteSelections,
                                                                         String selectedRouteKey,
                                                                         String selectedReason,
                                                                         String operatorId) {
        boolean structured = hasManualRouteSelections(manualRouteSelections);
        String selectedKey = structured ? null : textOrNull(selectedRouteKey);
        Map<String, Set<String>> selectedByBranch = structured
                ? selectedProgressionRoutes(routes, nodes, graph, selectedInitialRoutes, completedNodeKey,
                manualRouteSelections)
                : selectedProgressionRoute(routes, nodes, graph, selectedInitialRoutes, selectedKey);
        Set<String> branchNodeKeys = reachableProgressionBranchNodeKeys(nodes, graph, selectedInitialRoutes);
        requireManualBranches(graph, instance, nodes, tasks, branchNodeKeys, selectedByBranch,
                completedNodeKey, selectedReasonsByBranch(manualRouteSelections, selectedRouteKey, selectedReason),
                operatorId);
        return selectedByBranch;
    }

    public String selectedRouteKeyForBranch(String branchNodeKey,
                                            List<WorkflowManualRouteSelection> manualRouteSelections,
                                            String fallbackSelectedRouteKey) {
        if (!hasManualRouteSelections(manualRouteSelections)) {
            return textOrNull(fallbackSelectedRouteKey);
        }
        return manualRouteSelections.stream()
                .filter(selection -> branchNodeKey.equals(textOrNull(selection.branchNodeKey())))
                .findFirst()
                .map(selection -> requireText(selection.routeKey(),
                        "workflow manual branch selected route key must not be blank"))
                .orElse(null);
    }

    public String selectedReasonForRoute(WorkflowRouteInstance route,
                                         List<WorkflowManualRouteSelection> manualRouteSelections,
                                         String fallbackSelectedRouteKey,
                                         String fallbackSelectedReason) {
        if (!hasManualRouteSelections(manualRouteSelections)) {
            String selectedKey = textOrNull(fallbackSelectedRouteKey);
            return selectedKey != null && selectedKey.equals(route.getRouteKey()) ? fallbackSelectedReason : null;
        }
        return manualRouteSelections.stream()
                .filter(selection -> route.getSourceNodeKey().equals(textOrNull(selection.branchNodeKey())))
                .filter(selection -> route.getRouteKey().equals(textOrNull(selection.routeKey())))
                .findFirst()
                .map(WorkflowManualRouteSelection::selectedReason)
                .orElse(null);
    }

    private Map<String, Set<String>> selectedSubmitRoute(WorkflowRuntimeGraph graph,
                                                         String startNodeKey,
                                                         String selectedRouteKey) {
        Set<String> branchNodeKeys = reachableSubmitBranchNodeKeys(graph, startNodeKey);
        return graph.links().values().stream()
                .filter(link -> selectedRouteKey.equals(link.getRouteKey()))
                .filter(link -> branchNodeKeys.contains(link.getSourceNodeKey()))
                .findFirst()
                .map(link -> Map.of(link.getSourceNodeKey(), Set.of(selectedRouteKey)))
                .orElseThrow(() -> new PlatformException("workflow selected route is not candidate outgoing route"));
    }

    private Map<String, Set<String>> selectedSubmitRoutes(WorkflowRuntimeGraph graph,
                                                          String startNodeKey,
                                                          List<WorkflowManualRouteSelection> manualRouteSelections) {
        Set<String> branchNodeKeys = reachableSubmitBranchNodeKeys(graph, startNodeKey);
        Map<String, Set<String>> selected = new LinkedHashMap<>();
        for (WorkflowManualRouteSelection selection : manualRouteSelections) {
            String branchNodeKey = requireText(selection.branchNodeKey(),
                    "workflow manual branch node key must not be blank");
            String routeKey = requireText(selection.routeKey(),
                    "workflow manual branch selected route key must not be blank");
            if (!branchNodeKeys.contains(branchNodeKey) || graph.outgoing(branchNodeKey).stream()
                    .noneMatch(link -> routeKey.equals(link.getRouteKey()))) {
                throw new PlatformException("workflow selected route is not candidate outgoing route");
            }
            putSingleSelection(selected, branchNodeKey, routeKey);
        }
        return selected;
    }

    private Map<String, Set<String>> selectedProgressionRoute(List<WorkflowRouteInstance> routes,
                                                              List<WorkflowNodeInstance> nodes,
                                                              WorkflowRuntimeGraph graph,
                                                              List<WorkflowRouteInstance> selectedInitialRoutes,
                                                              String selectedRouteKey) {
        if (selectedRouteKey == null || selectedInitialRoutes.stream()
                .anyMatch(route -> selectedRouteKey.equals(route.getRouteKey()))) {
            return Map.of();
        }
        Set<String> branchNodeKeys = reachableProgressionBranchNodeKeys(nodes, graph, selectedInitialRoutes);
        return routes.stream()
                .filter(route -> route.getRouteStatus() == WorkflowRouteStatus.CANDIDATE)
                .filter(route -> selectedRouteKey.equals(route.getRouteKey()))
                .filter(route -> branchNodeKeys.contains(route.getSourceNodeKey()))
                .findFirst()
                .map(route -> Map.of(route.getSourceNodeKey(), Set.of(selectedRouteKey)))
                .orElseGet(Map::of);
    }

    private Map<String, Set<String>> selectedProgressionRoutes(List<WorkflowRouteInstance> routes,
                                                               List<WorkflowNodeInstance> nodes,
                                                               WorkflowRuntimeGraph graph,
                                                               List<WorkflowRouteInstance> selectedInitialRoutes,
                                                               String completedNodeKey,
                                                               List<WorkflowManualRouteSelection> manualRouteSelections) {
        Set<String> branchNodeKeys = reachableProgressionBranchNodeKeys(nodes, graph, selectedInitialRoutes);
        Map<String, Set<String>> selected = new LinkedHashMap<>();
        for (WorkflowManualRouteSelection selection : manualRouteSelections) {
            String branchNodeKey = requireText(selection.branchNodeKey(),
                    "workflow manual branch node key must not be blank");
            String routeKey = requireText(selection.routeKey(),
                    "workflow manual branch selected route key must not be blank");
            if (branchNodeKey.equals(completedNodeKey)) {
                continue;
            }
            boolean candidate = routes.stream()
                    .anyMatch(route -> route.getRouteStatus() == WorkflowRouteStatus.CANDIDATE
                            && branchNodeKey.equals(route.getSourceNodeKey())
                            && routeKey.equals(route.getRouteKey()));
            if (!branchNodeKeys.contains(branchNodeKey) || !candidate) {
                throw new PlatformException("workflow selected route is not candidate outgoing route");
            }
            putSingleSelection(selected, branchNodeKey, routeKey);
        }
        return selected;
    }

    private void requireManualBranches(WorkflowRuntimeGraph graph,
                                       WorkflowInstance instance,
                                       List<WorkflowNodeInstance> nodes,
                                       List<WorkflowTask> tasks,
                                       Set<String> branchNodeKeys,
                                       Map<String, Set<String>> selectedByBranch,
                                       String fallbackSelectorNodeKey,
                                       Map<String, String> selectedReasonsByBranch,
                                       String operatorId) {
        Map<String, WorkflowNodeInstance> runtimeNodes = nodesByKey(nodes);
        for (String branchNodeKey : branchNodeKeys) {
            WorkflowNodeDefinition graphNode = graph.requireNode(branchNodeKey);
            WorkflowNodeInstance runtimeNode = runtimeNodes.get(branchNodeKey);
            if (routeMode(graphNode, runtimeNode) != WorkflowRouteMode.MANUAL) {
                continue;
            }
            Set<String> selected = selectedByBranch.get(branchNodeKey);
            if (selected == null || selected.isEmpty()) {
                throw new PlatformException("workflow manual branch requires selected route: " + branchNodeKey);
            }
            WorkflowNodeDefinition governingNode = runtimeNode == null ? graphNode : nodeDefinition(runtimeNode);
            requireManualSelection(instance, governingNode, nodes, tasks, fallbackSelectorNodeKey,
                    selectedReasonsByBranch.getOrDefault(branchNodeKey, selectedReasonsByBranch.get("")),
                    operatorId);
        }
    }

    private void requireManualSelection(WorkflowInstance instance,
                                        WorkflowNodeDefinition branchNode,
                                        List<WorkflowNodeInstance> nodes,
                                        List<WorkflowTask> tasks,
                                        String fallbackSelectorNodeKey,
                                        String selectedReason,
                                        String operatorId) {
        if (Boolean.TRUE.equals(branchNode.getRequireManualSelectionReason()) && textOrNull(selectedReason) == null) {
            throw new PlatformException("workflow manual branch selection reason is required: "
                    + branchNode.getNodeKey());
        }
        String selectorNodeKey = textOrNull(branchNode.getSelectorNodeKey());
        if (selectorNodeKey == null) {
            selectorNodeKey = fallbackSelectorNodeKey;
        }
        selectorNodeKey = requireText(selectorNodeKey, "workflow manual branch selector node key must not be blank");
        String validOperatorId = requireText(operatorId, "workflow operator id must not be blank");
        WorkflowManualBranchSelectorResolver.SelectorResolution resolution = selectorResolver.resolve(instance, nodes,
                tasks, selectorNodeKey, null, validOperatorId);
        if (!resolution.selectable()) {
            throw manualSelectorException(branchNode.getNodeKey(), resolution);
        }
    }

    private PlatformException manualSelectorException(
            String branchNodeKey,
            WorkflowManualBranchSelectorResolver.SelectorResolution resolution) {
        String selectorNodeKey = resolution.selectorNodeKey();
        return switch (resolution.unselectableReason()) {
            case WorkflowManualBranchSelectorResolver.SELECTOR_NOT_FOUND ->
                    new PlatformException("workflow manual branch selector node not found: " + selectorNodeKey);
            case WorkflowManualBranchSelectorResolver.SELECTOR_UNSUPPORTED -> new PlatformException(
                    "workflow manual branch selector node must be start, approval or task: " + selectorNodeKey);
            case WorkflowManualBranchSelectorResolver.SELECTOR_NOT_PROCESSED -> new PlatformException(
                    "workflow manual branch selector has no actual processor: " + selectorNodeKey);
            default -> new PlatformException("workflow manual branch selector must be operator: " + branchNodeKey);
        };
    }

    private Set<String> reachableSubmitBranchNodeKeys(WorkflowRuntimeGraph graph, String startNodeKey) {
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(startNodeKey);
        Set<String> visited = new LinkedHashSet<>();
        Set<String> branchNodeKeys = new LinkedHashSet<>();
        while (!queue.isEmpty()) {
            String nodeKey = queue.removeFirst();
            if (!visited.add(nodeKey)) {
                continue;
            }
            WorkflowNodeDefinition node = graph.requireNode(nodeKey);
            if (node.getNodeType() == WorkflowNodeType.BRANCH) {
                branchNodeKeys.add(nodeKey);
                continue;
            }
            if (node.getNodeType() == WorkflowNodeType.APPROVAL || node.getNodeType() == WorkflowNodeType.TASK) {
                continue;
            }
            defaultRoutes(graph.outgoing(nodeKey)).forEach(link -> queue.addLast(link.getTargetNodeKey()));
        }
        return branchNodeKeys;
    }

    private Set<String> reachableProgressionBranchNodeKeys(List<WorkflowNodeInstance> nodes,
                                                           WorkflowRuntimeGraph graph,
                                                           List<WorkflowRouteInstance> selectedInitialRoutes) {
        Map<String, WorkflowNodeInstance> nodesByKey = nodesByKey(nodes);
        List<String> queue = selectedInitialRoutes.stream()
                .map(WorkflowRouteInstance::getTargetNodeKey)
                .collect(Collectors.toCollection(java.util.ArrayList::new));
        Set<String> visited = new LinkedHashSet<>();
        Set<String> branchNodeKeys = new LinkedHashSet<>();
        for (int index = 0; index < queue.size(); index++) {
            String nodeKey = queue.get(index);
            if (!visited.add(nodeKey)) {
                continue;
            }
            WorkflowNodeInstance node = nodesByKey.get(nodeKey);
            if (node == null) {
                continue;
            }
            if (node.getNodeType() == WorkflowNodeType.BRANCH) {
                branchNodeKeys.add(nodeKey);
                continue;
            }
            if (node.getNodeType() == WorkflowNodeType.APPROVAL || node.getNodeType() == WorkflowNodeType.TASK) {
                continue;
            }
            graph.outgoing(nodeKey).forEach(link -> queue.add(link.getTargetNodeKey()));
        }
        return branchNodeKeys;
    }

    private List<WorkflowLinkDefinition> defaultRoutes(List<WorkflowLinkDefinition> outgoing) {
        List<WorkflowLinkDefinition> defaults = outgoing.stream()
                .filter(link -> Boolean.TRUE.equals(link.getDefaultRoute()))
                .toList();
        return defaults.isEmpty() ? outgoing : defaults;
    }

    private WorkflowRouteMode routeMode(WorkflowNodeDefinition definition, WorkflowNodeInstance instance) {
        if (instance != null && instance.getRouteMode() != null) {
            return instance.getRouteMode();
        }
        return definition.getRouteMode() == null ? WorkflowRouteMode.AUTO : definition.getRouteMode();
    }

    private WorkflowRouteMode routeMode(WorkflowNodeInstance node) {
        return node.getRouteMode() == null ? WorkflowRouteMode.AUTO : node.getRouteMode();
    }

    private WorkflowNodeDefinition nodeDefinition(WorkflowNodeInstance node) {
        WorkflowNodeDefinition definition = new WorkflowNodeDefinition();
        definition.setNodeKey(node.getNodeKey());
        definition.setNodeType(node.getNodeType());
        definition.setRouteMode(node.getRouteMode());
        definition.setSelectorNodeKey(node.getSelectorNodeKey());
        definition.setRequireManualSelectionReason(node.getRequireManualSelectionReason());
        return definition;
    }

    private Map<String, WorkflowNodeInstance> nodesByKey(List<WorkflowNodeInstance> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Map.of();
        }
        return nodes.stream()
                .collect(Collectors.toMap(WorkflowNodeInstance::getNodeKey, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Map<String, String> selectedReasonsByBranch(List<WorkflowManualRouteSelection> manualRouteSelections,
                                                        String selectedRouteKey,
                                                        String selectedReason) {
        if (!hasManualRouteSelections(manualRouteSelections)) {
            if (textOrNull(selectedRouteKey) == null) {
                return Map.of();
            }
            Map<String, String> reasons = new LinkedHashMap<>();
            reasons.put("", selectedReason);
            return reasons;
        }
        Map<String, String> reasons = new LinkedHashMap<>();
        for (WorkflowManualRouteSelection selection : manualRouteSelections) {
            reasons.put(requireText(selection.branchNodeKey(), "workflow manual branch node key must not be blank"),
                    selection.selectedReason());
        }
        return reasons;
    }

    private String selectedReasonForBranch(String branchNodeKey,
                                           List<WorkflowManualRouteSelection> manualRouteSelections,
                                           String selectedRouteKey,
                                           String selectedReason) {
        if (!hasManualRouteSelections(manualRouteSelections)) {
            return textOrNull(selectedRouteKey) == null ? null : selectedReason;
        }
        return manualRouteSelections.stream()
                .filter(selection -> branchNodeKey.equals(textOrNull(selection.branchNodeKey())))
                .findFirst()
                .map(WorkflowManualRouteSelection::selectedReason)
                .orElse(null);
    }

    private void putSingleSelection(Map<String, Set<String>> selected, String branchNodeKey, String routeKey) {
        if (selected.put(branchNodeKey, Set.of(routeKey)) != null) {
            throw new PlatformException("workflow manual branch has duplicate route selection: " + branchNodeKey);
        }
    }

    private boolean hasManualRouteSelections(List<WorkflowManualRouteSelection> manualRouteSelections) {
        return manualRouteSelections != null && !manualRouteSelections.isEmpty();
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(message);
        }
        return value;
    }

    private String textOrNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
