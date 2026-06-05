package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.EntityLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkflowRuntimeProgressionService {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final WorkflowInstanceDao instanceDao;
    private final WorkflowNodeInstanceDao nodeDao;
    private final WorkflowRouteInstanceDao routeDao;
    private final WorkflowTaskDao taskDao;
    private final WorkflowEventDao eventDao;
    private final WorkflowRuntimeActivationService activationService;
    private final WorkflowInstanceStateService instanceStateService;
    private final WorkflowNodeInstanceStateService nodeStateService;
    private final WorkflowRouteInstanceStateService routeStateService;
    private final WorkflowRouteRuntimeService routeRuntimeService;
    private final WorkflowRuntimeTaskFactory taskFactory;
    private final WorkflowRuntimeEventFactory eventFactory;
    private final Optional<WorkflowApprovalSummaryWriter> approvalSummaryWriter;

    public WorkflowRuntimeProgressionService(WorkflowInstanceDao instanceDao,
                                             WorkflowNodeInstanceDao nodeDao,
                                             WorkflowRouteInstanceDao routeDao,
                                             WorkflowTaskDao taskDao,
                                             WorkflowEventDao eventDao,
                                             WorkflowRuntimeActivationService activationService,
                                             WorkflowInstanceStateService instanceStateService,
                                             WorkflowNodeInstanceStateService nodeStateService,
                                             WorkflowRouteInstanceStateService routeStateService,
                                             WorkflowRouteRuntimeService routeRuntimeService,
                                             WorkflowRuntimeTaskFactory taskFactory,
                                             WorkflowRuntimeEventFactory eventFactory,
                                             Optional<WorkflowApprovalSummaryWriter> approvalSummaryWriter) {
        this.instanceDao = instanceDao;
        this.nodeDao = nodeDao;
        this.routeDao = routeDao;
        this.taskDao = taskDao;
        this.eventDao = eventDao;
        this.activationService = activationService;
        this.instanceStateService = instanceStateService;
        this.nodeStateService = nodeStateService;
        this.routeStateService = routeStateService;
        this.routeRuntimeService = routeRuntimeService;
        this.taskFactory = taskFactory;
        this.eventFactory = eventFactory;
        this.approvalSummaryWriter = approvalSummaryWriter;
    }

    @Transactional
    public WorkflowProgressionResult advanceFromNode(String instanceId, String completedNodeKey,
                                                     String operatorId, Instant operatedAt) {
        return advanceFromNode(instanceId, completedNodeKey, operatorId, operatedAt, null);
    }

    @Transactional
    public WorkflowProgressionResult advanceFromNode(String instanceId, String completedNodeKey,
                                                     String operatorId, Instant operatedAt,
                                                     String selectedRouteKey) {
        WorkflowInstance instance = requireInstance(instanceId);
        if (instance.getInstanceStatus() != WorkflowInstanceStatus.RUNNING) {
            throw new PlatformException("workflow instance is not running: " + instanceId);
        }
        Instant now = operatedAt == null ? Instant.now() : operatedAt;
        String selectedKey = textOrNull(selectedRouteKey);
        List<WorkflowNodeInstance> nodes = nodes(instanceId);
        List<WorkflowRouteInstance> routes = routes(instanceId);
        WorkflowRuntimeGraph graph = graph(nodes, routes);
        List<WorkflowRouteInstance> selectedInitialRoutes = selectOutgoingRoutes(routes, nodes, graph,
                completedNodeKey, selectedKey);
        if (selectedInitialRoutes.isEmpty()) {
            return WorkflowProgressionResult.empty(instance);
        }
        Map<String, Set<String>> selectedRouteKeysByBranch = selectedRouteKeysByBranch(routes, nodes, graph,
                selectedInitialRoutes, selectedKey);

        List<WorkflowRouteInstance> droppedRoutes = dropUnselectedOutgoingRoutes(routes, completedNodeKey,
                selectedInitialRoutes, operatorId, now);
        List<WorkflowEvent> events = new ArrayList<>();
        for (WorkflowRouteInstance route : selectedInitialRoutes) {
            routeRuntimeService.effectiveRoute(route, routeReason(route, selectedKey), operatorId, now);
            events.add(eventFactory.routeSelected(instance, route, operatorId, now));
        }
        for (WorkflowRouteInstance route : droppedRoutes) {
            events.add(eventFactory.routeDropped(instance, route, operatorId, now));
        }
        Set<String> passedConvergeNodeKeys = handleConvergeArrivals(selectedInitialRoutes, routes, nodes, now);
        WorkflowActivationResult activation = activationService.activate(new WorkflowActivationRequest(
                graph,
                selectedInitialRoutes.stream()
                        .map(route -> new WorkflowActivationTarget(route.getTargetNodeKey(), route.getId()))
                        .toList(),
                selectedRouteKeysByBranch,
                passedConvergeNodeKeys,
                512
        ));
        applyManualBranchSelection(routes, selectedRouteKeysByBranch, operatorId, now);
        instanceStateService.applyActivation(instance, activation, now);
        nodeStateService.applyActivation(nodes, activation, now);
        routeStateService.applyActivation(routes, activation, operatorId, now);
        WorkflowRuntimeTaskDraft taskDraft = taskFactory.createBlockingTasks(instance, nodes, activation, operatorId, now);
        events.addAll(taskDraft.events());
        if (activation.completed()) {
            events.add(eventFactory.instanceCompleted(instance, operatorId, now));
        }
        if (activation.approvalCompleted()) {
            events.add(eventFactory.approvalCompleted(instance, operatorId, now));
            writeApprovalSummary(instance);
        }

        persist(instance, nodes, routes, taskDraft.tasks(), events, now);
        return new WorkflowProgressionResult(instance, activatedNodes(nodes, activation), selectedInitialRoutes,
                droppedRoutes, taskDraft.tasks(), events, activation);
    }

    private Set<String> handleConvergeArrivals(List<WorkflowRouteInstance> selectedRoutes,
                                               List<WorkflowRouteInstance> allRoutes,
                                               List<WorkflowNodeInstance> nodes,
                                               Instant now) {
        Map<String, WorkflowNodeInstance> nodesByKey = nodes.stream()
                .collect(Collectors.toMap(WorkflowNodeInstance::getNodeKey, Function.identity(), (left, right) -> left));
        Set<String> passed = new LinkedHashSet<>();
        for (WorkflowRouteInstance route : selectedRoutes) {
            WorkflowNodeInstance target = nodesByKey.get(route.getTargetNodeKey());
            if (target == null || target.getNodeType() != WorkflowNodeType.CONVERGE) {
                continue;
            }
            WorkflowConvergeDecision decision = routeRuntimeService.handleConvergeArrival(route, allRoutes,
                    target.getConvergeMode(), target.getConvergeRatio(), now);
            if (decision.passed()) {
                passed.add(target.getNodeKey());
            }
        }
        return passed;
    }

    private void persist(WorkflowInstance instance,
                         List<WorkflowNodeInstance> nodes,
                         List<WorkflowRouteInstance> routes,
                         List<WorkflowTask> tasks,
                         List<WorkflowEvent> events,
                         Instant now) {
        updateInstance(instance, now);
        nodes.forEach(node -> updateNode(node, now));
        routes.forEach(route -> updateRoute(route, now));
        tasks.forEach(task -> {
            EntityLifecycle.prepareInsert(task, now);
            taskDao.insert(task);
        });
        events.forEach(event -> {
            EntityLifecycle.prepareInsert(event, now);
            eventDao.insert(event);
        });
    }

    private void writeApprovalSummary(WorkflowInstance instance) {
        if (!Boolean.TRUE.equals(instance.getApprovalEnabled())) {
            return;
        }
        approvalSummaryWriter.ifPresent(writer -> writer.writeSubmitted(new WorkflowApprovalSummary(
                instance.getModuleAlias(),
                instance.getRecordId(),
                instance.getId(),
                instance.getApprovalStatus(),
                instance.getStartedBy(),
                instance.getStartedAt(),
                instance.getApprovalCompletedAt()
        )));
    }

    private WorkflowRuntimeGraph graph(List<WorkflowNodeInstance> nodes, List<WorkflowRouteInstance> routes) {
        return WorkflowRuntimeGraph.of(nodes.stream().map(this::nodeDefinition).toList(),
                routes.stream().map(this::linkDefinition).toList());
    }

    private WorkflowNodeDefinition nodeDefinition(WorkflowNodeInstance node) {
        WorkflowNodeDefinition definition = new WorkflowNodeDefinition();
        definition.setNodeKey(node.getNodeKey());
        definition.setNodeType(node.getNodeType());
        definition.setApprovalMode(node.getApprovalMode());
        definition.setApprovalRatio(node.getApprovalRatio());
        definition.setMilestoneType(node.getMilestoneType());
        definition.setConvergeMode(node.getConvergeMode());
        definition.setConvergeRatio(node.getConvergeRatio());
        return definition;
    }

    private WorkflowLinkDefinition linkDefinition(WorkflowRouteInstance route) {
        WorkflowLinkDefinition link = new WorkflowLinkDefinition();
        link.setRouteKey(route.getRouteKey());
        link.setSourceNodeKey(route.getSourceNodeKey());
        link.setTargetNodeKey(route.getTargetNodeKey());
        link.setDefaultRoute(route.getDefaultRoute());
        return link;
    }

    private List<WorkflowRouteInstance> selectOutgoingRoutes(List<WorkflowRouteInstance> routes,
                                                             List<WorkflowNodeInstance> nodes,
                                                             WorkflowRuntimeGraph graph,
                                                             String nodeKey,
                                                             String selectedRouteKey) {
        List<WorkflowRouteInstance> outgoing = routes.stream()
                .filter(route -> nodeKey.equals(route.getSourceNodeKey()))
                .filter(route -> route.getRouteStatus() == WorkflowRouteStatus.CANDIDATE)
                .toList();
        if (selectedRouteKey != null) {
            List<WorkflowRouteInstance> selected = outgoing.stream()
                    .filter(route -> selectedRouteKey.equals(route.getRouteKey()))
                    .toList();
            if (!selected.isEmpty()) {
                return selected;
            }
        }
        List<WorkflowRouteInstance> defaults = outgoing.stream()
                .filter(route -> Boolean.TRUE.equals(route.getDefaultRoute()))
                .toList();
        List<WorkflowRouteInstance> defaultSelection = defaults.isEmpty() ? outgoing : defaults;
        if (selectedRouteKey != null
                && !canUseSelectedRouteForReachableBranch(routes, nodes, graph, defaultSelection, selectedRouteKey)) {
            throw new PlatformException("workflow selected route is not candidate outgoing route");
        }
        return defaultSelection;
    }

    private Map<String, Set<String>> selectedRouteKeysByBranch(List<WorkflowRouteInstance> routes,
                                                               List<WorkflowNodeInstance> nodes,
                                                               WorkflowRuntimeGraph graph,
                                                               List<WorkflowRouteInstance> selectedInitialRoutes,
                                                               String selectedRouteKey) {
        if (selectedRouteKey == null || selectedInitialRoutes.stream()
                .anyMatch(route -> selectedRouteKey.equals(route.getRouteKey()))) {
            return Map.of();
        }
        Set<String> branchNodeKeys = reachableBranchNodeKeys(nodes, graph, selectedInitialRoutes);
        return routes.stream()
                .filter(route -> route.getRouteStatus() == WorkflowRouteStatus.CANDIDATE)
                .filter(route -> selectedRouteKey.equals(route.getRouteKey()))
                .filter(route -> branchNodeKeys.contains(route.getSourceNodeKey()))
                .findFirst()
                .map(route -> Map.of(route.getSourceNodeKey(), Set.of(selectedRouteKey)))
                .orElseGet(Map::of);
    }

    private boolean canUseSelectedRouteForReachableBranch(List<WorkflowRouteInstance> routes,
                                                          List<WorkflowNodeInstance> nodes,
                                                          WorkflowRuntimeGraph graph,
                                                          List<WorkflowRouteInstance> selectedInitialRoutes,
                                                          String selectedRouteKey) {
        Set<String> branchNodeKeys = reachableBranchNodeKeys(nodes, graph, selectedInitialRoutes);
        return routes.stream()
                .anyMatch(route -> route.getRouteStatus() == WorkflowRouteStatus.CANDIDATE
                        && selectedRouteKey.equals(route.getRouteKey())
                        && branchNodeKeys.contains(route.getSourceNodeKey()));
    }

    private Set<String> reachableBranchNodeKeys(List<WorkflowNodeInstance> nodes,
                                                WorkflowRuntimeGraph graph,
                                                List<WorkflowRouteInstance> selectedInitialRoutes) {
        Map<String, WorkflowNodeInstance> nodesByKey = nodes.stream()
                .collect(Collectors.toMap(WorkflowNodeInstance::getNodeKey, Function.identity(), (left, right) -> left));
        ArrayList<String> queue = selectedInitialRoutes.stream()
                .map(WorkflowRouteInstance::getTargetNodeKey)
                .collect(Collectors.toCollection(ArrayList::new));
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

    private void applyManualBranchSelection(List<WorkflowRouteInstance> routes,
                                            Map<String, Set<String>> selectedRouteKeysByBranch,
                                            String operatorId,
                                            Instant now) {
        if (selectedRouteKeysByBranch.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Set<String>> entry : selectedRouteKeysByBranch.entrySet()) {
            for (WorkflowRouteInstance route : routes) {
                if (!entry.getKey().equals(route.getSourceNodeKey())
                        || route.getRouteStatus() != WorkflowRouteStatus.CANDIDATE) {
                    continue;
                }
                if (entry.getValue().contains(route.getRouteKey())) {
                    routeRuntimeService.effectiveRoute(route, WorkflowRouteReason.MANUAL_SELECTED, operatorId, now);
                } else {
                    routeRuntimeService.ineffectiveRoute(route, WorkflowRouteReason.MANUAL_UNSELECTED,
                            operatorId, now);
                }
            }
        }
    }

    private List<WorkflowRouteInstance> dropUnselectedOutgoingRoutes(List<WorkflowRouteInstance> routes,
                                                                     String nodeKey,
                                                                     List<WorkflowRouteInstance> selected,
                                                                     String operatorId,
                                                                     Instant now) {
        Set<String> selectedIds = selected.stream().map(WorkflowRouteInstance::getId).collect(Collectors.toSet());
        List<WorkflowRouteInstance> dropped = new ArrayList<>();
        for (WorkflowRouteInstance route : routes) {
            if (!nodeKey.equals(route.getSourceNodeKey()) || route.getRouteStatus() != WorkflowRouteStatus.CANDIDATE
                    || selectedIds.contains(route.getId())) {
                continue;
            }
            routeRuntimeService.ineffectiveRoute(route, WorkflowRouteReason.MANUAL_UNSELECTED, operatorId, now);
            dropped.add(route);
        }
        return dropped;
    }

    private WorkflowRouteReason routeReason(WorkflowRouteInstance route, String selectedRouteKey) {
        return selectedRouteKey != null && selectedRouteKey.equals(route.getRouteKey())
                ? WorkflowRouteReason.MANUAL_SELECTED
                : Boolean.TRUE.equals(route.getDefaultRoute())
                ? WorkflowRouteReason.DEFAULT_SELECTED
                : WorkflowRouteReason.CONDITION_MATCHED;
    }

    private List<WorkflowNodeInstance> activatedNodes(List<WorkflowNodeInstance> nodes, WorkflowActivationResult activation) {
        Set<String> activated = Set.copyOf(activation.activatedNodeKeys());
        return nodes.stream().filter(node -> activated.contains(node.getNodeKey())).toList();
    }

    private WorkflowInstance requireInstance(String instanceId) {
        WorkflowInstance instance = instanceDao.findById(requireText(instanceId, "workflow instance id must not be blank"));
        if (instance == null) {
            throw new PlatformException("workflow instance not found: " + instanceId);
        }
        return instance;
    }

    private List<WorkflowNodeInstance> nodes(String instanceId) {
        return nodeDao.query(Criteria.of().eq("instanceId", instanceId), ALL);
    }

    private List<WorkflowRouteInstance> routes(String instanceId) {
        return routeDao.query(Criteria.of().eq("instanceId", instanceId), ALL);
    }

    private void updateInstance(WorkflowInstance instance, Instant now) {
        Integer expectedVersion = instance.getVersion();
        EntityLifecycle.prepareUpdate(instance, now, EntityLifecycle.nextVersion(expectedVersion));
        int updated = instanceDao.updateByIdAndVersion(instance, expectedVersion);
        if (updated <= 0) {
            throw new OptimisticLockException("workflow instance version conflict: " + instance.getId());
        }
    }

    private void updateNode(WorkflowNodeInstance node, Instant now) {
        Integer expectedVersion = node.getVersion();
        EntityLifecycle.prepareUpdate(node, now, EntityLifecycle.nextVersion(expectedVersion));
        int updated = nodeDao.updateByIdAndVersion(node, expectedVersion);
        if (updated <= 0) {
            throw new OptimisticLockException("workflow node version conflict: " + node.getId());
        }
    }

    private void updateRoute(WorkflowRouteInstance route, Instant now) {
        Integer expectedVersion = route.getVersion();
        EntityLifecycle.prepareUpdate(route, now, EntityLifecycle.nextVersion(expectedVersion));
        int updated = routeDao.updateByIdAndVersion(route, expectedVersion);
        if (updated <= 0) {
            throw new OptimisticLockException("workflow route version conflict: " + route.getId());
        }
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
