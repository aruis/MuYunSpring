package net.ximatai.muyun.spring.platform.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkflowRuntimeReadFacade {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<WorkflowRouteStatus> MANUAL_BRANCH_CANDIDATE_STATUSES = EnumSet.of(
            WorkflowRouteStatus.CANDIDATE,
            WorkflowRouteStatus.EFFECTIVE,
            WorkflowRouteStatus.INEFFECTIVE);
    private static final String ROUTE_ALREADY_DECIDED = "ROUTE_ALREADY_DECIDED";

    private final WorkflowInstanceDao instanceDao;
    private final WorkflowTaskDao taskDao;
    private final WorkflowNodeInstanceDao nodeDao;
    private final WorkflowRouteInstanceDao routeDao;
    private final WorkflowEventDao eventDao;
    private final WorkflowTaskActionAvailabilityService availabilityService;
    private final WorkflowActionPolicyService actionPolicyService;
    private final WorkflowTaskAssignmentPolicyService assignmentPolicyService;
    private final WorkflowUserTitleResolver userTitleResolver;
    private final WorkflowManualBranchSelectorResolver manualBranchSelectorResolver =
            new WorkflowManualBranchSelectorResolver();

    public WorkflowRuntimeReadFacade(WorkflowInstanceDao instanceDao,
                                     WorkflowTaskDao taskDao,
                                     WorkflowNodeInstanceDao nodeDao,
                                     WorkflowRouteInstanceDao routeDao,
                                     WorkflowEventDao eventDao,
                                     WorkflowTaskActionAvailabilityService availabilityService) {
        this(instanceDao, taskDao, nodeDao, routeDao, eventDao, availabilityService,
                new WorkflowActionPolicyService(), new WorkflowTaskAssignmentPolicyService(),
                WorkflowUserTitleResolver.NONE);
    }

    @Autowired
    public WorkflowRuntimeReadFacade(WorkflowInstanceDao instanceDao,
                                     WorkflowTaskDao taskDao,
                                     WorkflowNodeInstanceDao nodeDao,
                                     WorkflowRouteInstanceDao routeDao,
                                     WorkflowEventDao eventDao,
                                     WorkflowTaskActionAvailabilityService availabilityService,
                                     WorkflowActionPolicyService actionPolicyService,
                                     WorkflowTaskAssignmentPolicyService assignmentPolicyService,
                                     ObjectProvider<WorkflowUserTitleResolver> userTitleResolver) {
        this(instanceDao, taskDao, nodeDao, routeDao, eventDao, availabilityService, actionPolicyService,
                assignmentPolicyService, userTitleResolver == null
                ? WorkflowUserTitleResolver.NONE
                : userTitleResolver.getIfAvailable(() -> WorkflowUserTitleResolver.NONE));
    }

    public WorkflowRuntimeReadFacade(WorkflowInstanceDao instanceDao,
                                     WorkflowTaskDao taskDao,
                                     WorkflowNodeInstanceDao nodeDao,
                                     WorkflowRouteInstanceDao routeDao,
                                     WorkflowEventDao eventDao,
                                     WorkflowTaskActionAvailabilityService availabilityService,
                                     WorkflowActionPolicyService actionPolicyService,
                                     WorkflowTaskAssignmentPolicyService assignmentPolicyService,
                                     WorkflowUserTitleResolver userTitleResolver) {
        this.instanceDao = instanceDao;
        this.taskDao = taskDao;
        this.nodeDao = nodeDao;
        this.routeDao = routeDao;
        this.eventDao = eventDao;
        this.availabilityService = availabilityService;
        this.actionPolicyService = actionPolicyService == null
                ? new WorkflowActionPolicyService()
                : actionPolicyService;
        this.assignmentPolicyService = assignmentPolicyService == null
                ? new WorkflowTaskAssignmentPolicyService()
                : assignmentPolicyService;
        this.userTitleResolver = userTitleResolver == null ? WorkflowUserTitleResolver.NONE : userTitleResolver;
    }

    public WorkflowRuntimeReadFacade(WorkflowInstanceDao instanceDao,
                                     WorkflowTaskDao taskDao,
                                     WorkflowNodeInstanceDao nodeDao,
                                     WorkflowRouteInstanceDao routeDao,
                                     WorkflowEventDao eventDao,
                                     WorkflowTaskActionAvailabilityService availabilityService,
                                     WorkflowActionPolicyService actionPolicyService) {
        this(instanceDao, taskDao, nodeDao, routeDao, eventDao, availabilityService, actionPolicyService,
                new WorkflowTaskAssignmentPolicyService(), WorkflowUserTitleResolver.NONE);
    }

    public WorkflowRuntimeRenderBundle renderBundle(String instanceId) {
        WorkflowInstance instance = requireInstance(instanceId);
        actionPolicyService.requireRecordView(instance);
        List<WorkflowNodeInstance> nodes = nodeDao.query(Criteria.of().eq("instanceId", instance.getId()),
                ALL, Sort.asc("createdAt"));
        List<WorkflowRouteInstance> routes = routeDao.query(Criteria.of().eq("instanceId", instance.getId()),
                ALL, Sort.asc("createdAt"));
        return new WorkflowRuntimeRenderBundle("RUNTIME", instance, nodes, routes);
    }

    public List<WorkflowManualBranchCandidateView> manualBranchCandidates(String instanceId) {
        WorkflowInstance instance = requireInstance(instanceId);
        actionPolicyService.requireRecordView(instance);
        List<WorkflowNodeInstance> nodes = nodeDao.query(Criteria.of().eq("instanceId", instance.getId()),
                ALL, Sort.asc("createdAt"));
        List<WorkflowRouteInstance> routes = routeDao.query(Criteria.of().eq("instanceId", instance.getId()),
                ALL, Sort.asc("createdAt"));
        Map<String, WorkflowNodeInstance> nodeByKey = nodes.stream()
                .collect(Collectors.toMap(WorkflowNodeInstance::getNodeKey, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
        Map<String, List<WorkflowRouteInstance>> routesBySourceNodeKey = routes.stream()
                .filter(route -> MANUAL_BRANCH_CANDIDATE_STATUSES.contains(route.getRouteStatus()))
                .collect(Collectors.groupingBy(WorkflowRouteInstance::getSourceNodeKey, LinkedHashMap::new,
                        Collectors.toList()));
        return nodes.stream()
                .filter(node -> node.getNodeType() == WorkflowNodeType.BRANCH)
                .filter(node -> node.getRouteMode() == WorkflowRouteMode.MANUAL)
                .sorted(nodeSort())
                .map(node -> manualBranchCandidate(node, routesBySourceNodeKey.getOrDefault(node.getNodeKey(),
                        List.of()), nodeByKey))
                .toList();
    }

    public List<WorkflowManualBranchCandidatePrecheckView> manualBranchCandidatePrechecks(String instanceId,
                                                                                          String operatorId) {
        WorkflowInstance instance = requireInstance(instanceId);
        actionPolicyService.requireRecordView(instance);
        String validOperatorId = requireOperator(operatorId);
        List<WorkflowNodeInstance> nodes = nodeDao.query(Criteria.of().eq("instanceId", instance.getId()),
                ALL, Sort.asc("createdAt"));
        List<WorkflowRouteInstance> routes = routeDao.query(Criteria.of().eq("instanceId", instance.getId()),
                ALL, Sort.asc("createdAt"));
        List<WorkflowTask> tasks = taskDao.query(Criteria.of().eq("instanceId", instance.getId()),
                ALL, Sort.asc("createdAt"));
        Map<String, WorkflowNodeInstance> nodeByKey = nodes.stream()
                .collect(Collectors.toMap(WorkflowNodeInstance::getNodeKey, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
        Map<String, List<WorkflowRouteInstance>> routesBySourceNodeKey = routes.stream()
                .filter(route -> MANUAL_BRANCH_CANDIDATE_STATUSES.contains(route.getRouteStatus()))
                .collect(Collectors.groupingBy(WorkflowRouteInstance::getSourceNodeKey, LinkedHashMap::new,
                        Collectors.toList()));
        return nodes.stream()
                .filter(node -> node.getNodeType() == WorkflowNodeType.BRANCH)
                .filter(node -> node.getRouteMode() == WorkflowRouteMode.MANUAL)
                .sorted(nodeSort())
                .map(node -> manualBranchCandidatePrecheck(instance, node, routesBySourceNodeKey.getOrDefault(
                        node.getNodeKey(), List.of()), nodes, tasks, nodeByKey, validOperatorId))
                .toList();
    }

    public List<WorkflowTask> instanceTasks(String instanceId) {
        WorkflowInstance instance = requireInstance(instanceId);
        actionPolicyService.requireRecordView(instance);
        return taskDao.query(Criteria.of().eq("instanceId", instance.getId()), ALL, Sort.asc("createdAt"));
    }

    public List<WorkflowEvent> instanceEvents(String instanceId) {
        WorkflowInstance instance = requireInstance(instanceId);
        actionPolicyService.requireRecordView(instance);
        return eventDao.query(Criteria.of().eq("instanceId", instance.getId()), ALL,
                Sort.asc("occurredAt"), Sort.asc("createdAt"));
    }

    public List<WorkflowRuntimeAddSignExplanationView> addSignExplanations(String instanceId) {
        WorkflowInstance instance = requireInstance(instanceId);
        actionPolicyService.requireRecordView(instance);
        List<WorkflowNodeInstance> nodes = nodeDao.query(Criteria.of().eq("instanceId", instance.getId()),
                ALL, Sort.asc("createdAt"));
        List<WorkflowRouteInstance> routes = routeDao.query(Criteria.of().eq("instanceId", instance.getId()),
                ALL, Sort.asc("createdAt"));
        Map<String, WorkflowNodeInstance> nodesByKey = nodes.stream()
                .filter(node -> node.getNodeKey() != null)
                .collect(Collectors.toMap(WorkflowNodeInstance::getNodeKey, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
        List<WorkflowRuntimeAddSignExplanationView> views = new ArrayList<>();
        nodes.stream()
                .filter(node -> Boolean.TRUE.equals(node.getAddedByAddSign()))
                .sorted(nodeSort())
                .map(node -> addSignNodeExplanation(node, nodesByKey))
                .forEach(views::add);
        routes.stream()
                .filter(route -> Boolean.TRUE.equals(route.getAddedByAddSign()))
                .sorted(routeSort())
                .map(route -> addSignRouteExplanation(route, nodesByKey))
                .forEach(views::add);
        return List.copyOf(views);
    }

    public List<WorkflowTaskAvailableAction> instanceAvailableActions(String instanceId, String operatorId) {
        WorkflowInstance instance = requireInstance(instanceId);
        actionPolicyService.requireRecordView(instance);
        String validOperatorId = requireOperator(operatorId);
        Map<String, WorkflowNodeInstance> nodes = nodeById(instance.getId());
        return taskDao.query(Criteria.of().eq("instanceId", instance.getId()), ALL, Sort.asc("createdAt"))
                .stream()
                .filter(task -> task.getTaskStatus() == WorkflowTaskStatus.TODO)
                .filter(task -> assignmentPolicyService.canProcess(task, validOperatorId))
                .flatMap(task -> availabilityService.availableActions(task.getId(), validOperatorId).stream()
                        .map(action -> enrich(action, task, nodes.get(task.getNodeInstanceId()))))
                .toList();
    }

    public List<WorkflowWorkbenchCard> todoCards(String assigneeId, PageRequest pageRequest) {
        return todoCards(assigneeId, pageRequest, WorkflowWorkbenchQueryRequest.empty());
    }

    public List<WorkflowWorkbenchCard> todoCards(String assigneeId, PageRequest pageRequest,
                                                 WorkflowWorkbenchQueryRequest request) {
        String validAssigneeId = requireText(assigneeId, "workflow assignee id must not be blank");
        List<WorkflowTask> tasks = taskDao.query(Criteria.of()
                        .eq("taskStatus", WorkflowTaskStatus.TODO)
                        .in("taskKind", List.of(WorkflowTaskKind.APPROVAL, WorkflowTaskKind.BUSINESS,
                                WorkflowTaskKind.RESUBMIT)),
                ALL, Sort.asc("dueAt"), Sort.desc("createdAt")).stream()
                .filter(task -> assignmentPolicyService.canSeeTodo(task, validAssigneeId))
                .toList();
        return pageItems(cards("TODO", tasks, request), page(pageRequest));
    }

    public List<WorkflowWorkbenchCard> doneCards(String processorId, PageRequest pageRequest) {
        return doneCards(processorId, pageRequest, WorkflowWorkbenchQueryRequest.empty());
    }

    public List<WorkflowWorkbenchCard> doneCards(String processorId, PageRequest pageRequest,
                                                 WorkflowWorkbenchQueryRequest request) {
        String validProcessorId = requireText(processorId, "workflow processor id must not be blank");
        List<WorkflowTask> tasks = taskDao.query(Criteria.of()
                        .in("taskStatus", List.of(WorkflowTaskStatus.DONE, WorkflowTaskStatus.REJECTED,
                                WorkflowTaskStatus.ROLLED_BACK, WorkflowTaskStatus.TRANSFERRED)),
                ALL, Sort.desc("completedAt"), Sort.desc("updatedAt")).stream()
                .filter(task -> validProcessorId.equals(task.getActualProcessorId())
                        || validProcessorId.equals(task.getTransferredBy()))
                .toList();
        return pageItems(cards("DONE", tasks, request), page(pageRequest));
    }

    public List<WorkflowWorkbenchCard> noticeCards(String assigneeId, PageRequest pageRequest) {
        return noticeCards(assigneeId, pageRequest, WorkflowWorkbenchQueryRequest.empty());
    }

    public List<WorkflowWorkbenchCard> noticeCards(String assigneeId, PageRequest pageRequest,
                                                   WorkflowWorkbenchQueryRequest request) {
        String validAssigneeId = requireText(assigneeId, "workflow assignee id must not be blank");
        List<WorkflowTask> tasks = taskDao.query(Criteria.of()
                        .eq("assigneeId", validAssigneeId)
                        .eq("taskKind", WorkflowTaskKind.NOTICE)
                        .in("taskStatus", List.of(WorkflowTaskStatus.TODO, WorkflowTaskStatus.NOTICED)),
                ALL, Sort.desc("createdAt"));
        return pageItems(cards("NOTICE", tasks, request), page(pageRequest));
    }

    public WorkflowWorkbenchStats workbenchStats(String boardType, String operatorId) {
        return workbenchStats(boardType, operatorId, WorkflowWorkbenchQueryRequest.empty());
    }

    public WorkflowWorkbenchStats workbenchStats(String boardType, String operatorId,
                                                 WorkflowWorkbenchQueryRequest request) {
        String normalizedBoard = requireText(boardType, "workflow workbench board type must not be blank")
                .toUpperCase();
        String validOperatorId = requireText(operatorId, "workflow operator id must not be blank");
        WorkflowWorkbenchQueryRequest filters = filtersOnly(request);
        return switch (normalizedBoard) {
            case "TRACKING" -> trackingStats(validOperatorId, filters);
            case "TODO" -> todoStats(validOperatorId, filters);
            case "DONE" -> doneStats(validOperatorId, filters);
            case "NOTICE" -> noticeStats(validOperatorId, filters);
            case "DELEGATION" -> delegationStats(validOperatorId, filters);
            default -> throw new PlatformException("unsupported workflow workbench board type: " + boardType);
        };
    }

    private WorkflowWorkbenchStats trackingStats(String starterId, WorkflowWorkbenchQueryRequest request) {
        List<WorkflowWorkbenchCard> cards = trackingCards(starterId, ALL, request);
        EnumMap<WorkflowInstanceStatus, Long> counts = new EnumMap<>(WorkflowInstanceStatus.class);
        cards.forEach(card -> counts.merge(card.instanceStatus(), 1L, Long::sum));
        return new WorkflowWorkbenchStats("TRACKING", statsWithAll(cards.size(), WorkflowInstanceStatus.values(), counts));
    }

    private WorkflowManualBranchCandidateView manualBranchCandidate(WorkflowNodeInstance node,
                                                                    List<WorkflowRouteInstance> routes,
                                                                    Map<String, WorkflowNodeInstance> nodeByKey) {
        List<WorkflowManualBranchCandidateView.Candidate> candidates = routes.stream()
                .sorted(routeSort())
                .map(route -> {
                    WorkflowNodeInstance target = nodeByKey.get(route.getTargetNodeKey());
                    return new WorkflowManualBranchCandidateView.Candidate(
                            route.getId(),
                            route.getRouteKey(),
                            route.getTargetNodeKey(),
                            target == null ? null : target.getNodeType(),
                            route.getRouteStatus(),
                            route.getDefaultRoute());
                })
                .toList();
        return new WorkflowManualBranchCandidateView(
                node.getNodeKey(),
                node.getRouteMode(),
                node.getSelectorNodeKey(),
                node.getRequireManualSelectionReason(),
                candidates);
    }

    private WorkflowManualBranchCandidatePrecheckView manualBranchCandidatePrecheck(WorkflowInstance instance,
                                                                                    WorkflowNodeInstance node,
                                                                                    List<WorkflowRouteInstance> routes,
                                                                                    List<WorkflowNodeInstance> nodes,
                                                                                    List<WorkflowTask> tasks,
                                                                                    Map<String, WorkflowNodeInstance> nodeByKey,
                                                                                    String operatorId) {
        WorkflowManualBranchSelectorResolver.SelectorResolution selectorResolution =
                manualBranchSelectorResolver.resolve(instance, nodes, tasks, node.getSelectorNodeKey(),
                        node.getNodeKey(), operatorId);
        boolean hasSelectableRoute = routes.stream()
                .anyMatch(route -> route.getRouteStatus() == WorkflowRouteStatus.CANDIDATE);
        boolean branchSelectable = selectorResolution.selectable() && hasSelectableRoute;
        String branchUnselectableReason = branchSelectable
                ? null
                : firstText(selectorResolution.unselectableReason(), ROUTE_ALREADY_DECIDED);
        List<WorkflowManualBranchCandidatePrecheckView.Candidate> candidates = routes.stream()
                .sorted(routeSort())
                .map(route -> manualBranchRouteCandidate(route, nodeByKey, selectorResolution))
                .toList();
        return new WorkflowManualBranchCandidatePrecheckView(
                node.getNodeKey(),
                node.getRouteMode(),
                selectorResolution.selectorNodeKey(),
                node.getRequireManualSelectionReason(),
                selectorResolution.resolvedUserId(),
                operatorId,
                branchSelectable,
                branchUnselectableReason,
                candidates);
    }

    private WorkflowManualBranchCandidatePrecheckView.Candidate manualBranchRouteCandidate(
            WorkflowRouteInstance route,
            Map<String, WorkflowNodeInstance> nodeByKey,
            WorkflowManualBranchSelectorResolver.SelectorResolution selectorResolution) {
        WorkflowNodeInstance target = nodeByKey.get(route.getTargetNodeKey());
        boolean routeSelectable = selectorResolution.selectable()
                && route.getRouteStatus() == WorkflowRouteStatus.CANDIDATE;
        String routeUnselectableReason = routeSelectable
                ? null
                : routeUnselectableReason(route, selectorResolution);
        return new WorkflowManualBranchCandidatePrecheckView.Candidate(
                route.getId(),
                route.getRouteKey(),
                route.getTargetNodeKey(),
                target == null ? null : target.getNodeType(),
                route.getRouteStatus(),
                route.getDefaultRoute(),
                routeSelectable,
                routeUnselectableReason);
    }

    private String routeUnselectableReason(WorkflowRouteInstance route,
                                           WorkflowManualBranchSelectorResolver.SelectorResolution selectorResolution) {
        if (route.getRouteStatus() != WorkflowRouteStatus.CANDIDATE) {
            return ROUTE_ALREADY_DECIDED;
        }
        return selectorResolution.unselectableReason();
    }

    private WorkflowRuntimeAddSignExplanationView addSignNodeExplanation(
            WorkflowNodeInstance node,
            Map<String, WorkflowNodeInstance> nodesByKey) {
        return new WorkflowRuntimeAddSignExplanationView(
                WorkflowRuntimeAddSignExplanationView.ORIGIN_TYPE_ADD_SIGN,
                WorkflowRuntimeAddSignExplanationView.DIMENSION_NODE,
                Boolean.FALSE,
                node.getId(),
                node.getNodeKey(),
                node.getNodeType(),
                node.getNodeStatus(),
                null,
                null,
                null,
                null,
                null,
                node.getAddSignSourceNodeKey(),
                sourceNodeName(node.getAddSignSourceNodeKey(), nodesByKey),
                node.getAddSignOperatorId(),
                node.getAddSignAt());
    }

    private WorkflowRuntimeAddSignExplanationView addSignRouteExplanation(
            WorkflowRouteInstance route,
            Map<String, WorkflowNodeInstance> nodesByKey) {
        return new WorkflowRuntimeAddSignExplanationView(
                WorkflowRuntimeAddSignExplanationView.ORIGIN_TYPE_ADD_SIGN,
                WorkflowRuntimeAddSignExplanationView.DIMENSION_ROUTE,
                Boolean.TRUE,
                null,
                null,
                null,
                null,
                route.getId(),
                route.getRouteKey(),
                route.getSourceNodeKey(),
                route.getTargetNodeKey(),
                route.getRouteStatus(),
                route.getAddSignSourceNodeKey(),
                sourceNodeName(route.getAddSignSourceNodeKey(), nodesByKey),
                route.getAddSignOperatorId(),
                route.getAddSignAt());
    }

    private String sourceNodeName(String sourceNodeKey, Map<String, WorkflowNodeInstance> nodesByKey) {
        String validSourceNodeKey = blankToNull(sourceNodeKey);
        if (validSourceNodeKey == null) {
            return null;
        }
        WorkflowNodeInstance sourceNode = nodesByKey.get(validSourceNodeKey);
        return firstText(snapshotText(sourceNode, "nodeName"),
                firstText(snapshotText(sourceNode, "name"),
                        firstText(snapshotText(sourceNode, "title"), validSourceNodeKey)));
    }

    private String snapshotText(WorkflowNodeInstance node, String fieldName) {
        if (node == null || node.getNodeSnapshotText() == null || node.getNodeSnapshotText().isBlank()) {
            return null;
        }
        try {
            JsonNode value = OBJECT_MAPPER.readTree(node.getNodeSnapshotText()).path(fieldName);
            if (value.isMissingNode() || value.isNull()) {
                return null;
            }
            return blankToNull(value.asText(null));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Comparator<WorkflowNodeInstance> nodeSort() {
        return Comparator
                .comparing(WorkflowNodeInstance::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(WorkflowNodeInstance::getNodeKey, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private Comparator<WorkflowRouteInstance> routeSort() {
        return Comparator
                .comparing(WorkflowRouteInstance::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(WorkflowRouteInstance::getRouteKey, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private WorkflowWorkbenchStats todoStats(String assigneeId, WorkflowWorkbenchQueryRequest request) {
        List<WorkflowWorkbenchCard> cards = todoCards(assigneeId, ALL, request);
        EnumMap<WorkflowOvertimeStatus, Long> counts = new EnumMap<>(WorkflowOvertimeStatus.class);
        cards.forEach(card -> counts.merge(overtimeStatus(card), 1L, Long::sum));
        return new WorkflowWorkbenchStats("TODO", statsWithAll(cards.size(), WorkflowOvertimeStatus.values(), counts));
    }

    private WorkflowWorkbenchStats doneStats(String processorId, WorkflowWorkbenchQueryRequest request) {
        List<WorkflowWorkbenchCard> cards = doneCards(processorId, ALL, request);
        Map<String, Long> counts = new LinkedHashMap<>();
        cards.forEach(card -> counts.merge(doneStatCode(card), 1L, Long::sum));
        return new WorkflowWorkbenchStats("DONE", List.of(
                stat("ALL", "全部", cards.size()),
                stat("DONE", WorkflowTaskStatus.DONE.getTitle(), counts.getOrDefault("DONE", 0L)),
                stat("REJECTED", WorkflowTaskStatus.REJECTED.getTitle(), counts.getOrDefault("REJECTED", 0L)),
                stat("ROLLED_BACK", WorkflowTaskStatus.ROLLED_BACK.getTitle(), counts.getOrDefault("ROLLED_BACK", 0L)),
                stat("TRANSFERRED", WorkflowTaskStatus.TRANSFERRED.getTitle(), counts.getOrDefault("TRANSFERRED", 0L))
        ));
    }

    private WorkflowWorkbenchStats noticeStats(String assigneeId, WorkflowWorkbenchQueryRequest request) {
        List<WorkflowWorkbenchCard> cards = noticeCards(assigneeId, ALL, request);
        EnumMap<WorkflowNoticeReadStatus, Long> counts = new EnumMap<>(WorkflowNoticeReadStatus.class);
        cards.forEach(card -> counts.merge(card.readStatus(), 1L, Long::sum));
        return new WorkflowWorkbenchStats("NOTICE", List.of(
                stat(WorkflowNoticeReadStatus.ALL, cards.size()),
                stat(WorkflowNoticeReadStatus.UNREAD, counts.getOrDefault(WorkflowNoticeReadStatus.UNREAD, 0L)),
                stat(WorkflowNoticeReadStatus.READ, counts.getOrDefault(WorkflowNoticeReadStatus.READ, 0L))
        ));
    }

    private WorkflowWorkbenchStats delegationStats(String principalId, WorkflowWorkbenchQueryRequest request) {
        List<WorkflowWorkbenchCard> cards = delegationCards(principalId, ALL, request);
        EnumMap<WorkflowOvertimeStatus, Long> counts = new EnumMap<>(WorkflowOvertimeStatus.class);
        cards.forEach(card -> counts.merge(overtimeStatus(card), 1L, Long::sum));
        return new WorkflowWorkbenchStats("DELEGATION", statsWithAll(cards.size(), WorkflowOvertimeStatus.values(), counts));
    }

    public List<WorkflowWorkbenchCard> trackingCards(String starterId, PageRequest pageRequest) {
        return trackingCards(starterId, pageRequest, WorkflowWorkbenchQueryRequest.empty());
    }

    public List<WorkflowWorkbenchCard> trackingCards(String starterId, PageRequest pageRequest,
                                                     WorkflowWorkbenchQueryRequest request) {
        String validStarterId = requireText(starterId, "workflow starter id must not be blank");
        List<WorkflowInstance> instances = instanceDao.query(Criteria.of().eq("startedBy", validStarterId),
                ALL, Sort.desc("startedAt"), Sort.desc("updatedAt"));
        List<WorkflowWorkbenchCard> cards = instances.stream()
                .map(instance -> card("TRACKING", instance, null, currentAddSignNode(instance),
                        currentAssignees(instance.getId())))
                .filter(card -> matches(card, request))
                .sorted(sorter("TRACKING", request))
                .toList();
        return pageItems(cards, page(pageRequest));
    }

    public List<WorkflowWorkbenchCard> delegationCards(String principalId, PageRequest pageRequest) {
        return delegationCards(principalId, pageRequest, WorkflowWorkbenchQueryRequest.empty());
    }

    public List<WorkflowWorkbenchCard> delegationCards(String principalId, PageRequest pageRequest,
                                                       WorkflowWorkbenchQueryRequest request) {
        String validPrincipalId = requireText(principalId, "workflow delegation principal id must not be blank");
        List<WorkflowTask> tasks = taskDao.query(Criteria.of()
                        .eq("assignmentKind", WorkflowAssignmentKind.DELEGATED)
                        .eq("delegatedFromUserId", validPrincipalId)
                        .eq("taskStatus", WorkflowTaskStatus.TODO),
                ALL, Sort.asc("dueAt"), Sort.desc("createdAt"));
        Map<String, List<WorkflowTask>> tasksByInstance = new LinkedHashMap<>();
        for (WorkflowTask task : tasks) {
            tasksByInstance.computeIfAbsent(task.getInstanceId(), ignored -> new java.util.ArrayList<>()).add(task);
        }
        List<WorkflowWorkbenchCard> cards = tasksByInstance.values().stream()
                .map(this::delegationCard)
                .filter(card -> matches(card, request))
                .sorted(sorter("DELEGATION", request))
                .toList();
        return pageItems(cards, page(pageRequest));
    }

    private List<WorkflowWorkbenchCard> cards(String boardType, List<WorkflowTask> tasks) {
        return cards(boardType, tasks, WorkflowWorkbenchQueryRequest.empty());
    }

    private List<WorkflowWorkbenchCard> cards(String boardType, List<WorkflowTask> tasks,
                                              WorkflowWorkbenchQueryRequest request) {
        Map<String, WorkflowInstance> instances = new LinkedHashMap<>();
        Map<String, WorkflowNodeInstance> nodes = new LinkedHashMap<>();
        for (WorkflowTask task : tasks) {
            instances.computeIfAbsent(task.getInstanceId(), instanceDao::findById);
            if (task.getNodeInstanceId() != null) {
                nodes.computeIfAbsent(task.getNodeInstanceId(), nodeDao::findById);
            }
        }
        Map<String, String> userTitles = userTitles(tasks);
        return tasks.stream()
                .map(task -> card(boardType, instances.get(task.getInstanceId()), task,
                        nodes.get(task.getNodeInstanceId()), assigneeIds(task), userTitles))
                .filter(card -> matches(card, request))
                .sorted(sorter(boardType, request))
                .toList();
    }

    private List<String> assigneeIds(WorkflowTask task) {
        if (task == null || task.getAssigneeId() == null || task.getAssigneeId().isBlank()) {
            return List.of();
        }
        if (task.getAssignmentKind() == WorkflowAssignmentKind.DELEGATED
                && Boolean.TRUE.equals(task.getPrincipalCanProcess())
                && task.getDelegatedFromUserId() != null
                && !task.getDelegatedFromUserId().isBlank()
                && task.getTransferredFromUserId() == null) {
            return List.of(task.getAssigneeId(), task.getDelegatedFromUserId());
        }
        return List.of(task.getAssigneeId());
    }

    private WorkflowWorkbenchCard card(String boardType,
                                       WorkflowInstance instance,
                                       WorkflowTask task,
                                       WorkflowNodeInstance node,
                                       List<String> currentAssigneeIds) {
        return card(boardType, instance, task, node, currentAssigneeIds, (Map<String, String>) null);
    }

    private WorkflowWorkbenchCard delegationCard(List<WorkflowTask> tasks) {
        WorkflowTask representative = tasks.stream()
                .sorted(Comparator.comparing(WorkflowTask::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .findFirst()
                .orElseThrow(() -> new PlatformException("workflow delegation task must not be empty"));
        WorkflowInstance instance = instanceDao.findById(representative.getInstanceId());
        WorkflowNodeInstance node = representative.getNodeInstanceId() == null
                ? null
                : nodeDao.findById(representative.getNodeInstanceId());
        List<String> currentAssigneeIds = tasks.stream()
                .flatMap(task -> assigneeIds(task).stream())
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        return card("DELEGATION", instance, representative, node, currentAssigneeIds, userTitles(tasks), tasks.size());
    }

    private WorkflowWorkbenchCard card(String boardType,
                                       WorkflowInstance instance,
                                       WorkflowTask task,
                                       WorkflowNodeInstance node,
                                       List<String> currentAssigneeIds,
                                       Map<String, String> userTitles) {
        return card(boardType, instance, task, node, currentAssigneeIds, userTitles, null);
    }

    private WorkflowWorkbenchCard card(String boardType,
                                       WorkflowInstance instance,
                                       WorkflowTask task,
                                       WorkflowNodeInstance node,
                                       List<String> currentAssigneeIds,
                                       Integer delegationTaskCount) {
        return card(boardType, instance, task, node, currentAssigneeIds, userTitles(task), delegationTaskCount);
    }

    private WorkflowWorkbenchCard card(String boardType,
                                       WorkflowInstance instance,
                                       WorkflowTask task,
                                       WorkflowNodeInstance node,
                                       List<String> currentAssigneeIds,
                                       Map<String, String> userTitles,
                                       Integer delegationTaskCount) {
        if (instance == null) {
            throw new PlatformException("workflow instance not found: " + (task == null ? null : task.getInstanceId()));
        }
        return new WorkflowWorkbenchCard(boardType, instance.getId(), instance.getModuleAlias(), instance.getRecordId(),
                instance.getDefinitionId(), instance.getWorkflowVersionId(),
                instance.getInstanceStatus(), instance.getApprovalStatus(), task == null ? null : task.getId(),
                task == null ? null : task.getTaskKind(), task == null ? null : task.getTaskStatus(),
                node == null ? null : node.getNodeKey(), nodeTitle(node), instance.getCurrentNodeKeys(),
                currentAssigneeIds, titles(currentAssigneeIds, userTitles),
                instance.getStartedAt(), task == null ? instance.getStartedAt() : task.getCreatedAt(),
                task == null ? instance.getCompletedAt() : task.getCompletedAt(),
                task == null ? instance.getLastActionCode() : actionCode(task),
                node == null ? null : node.getOvertimeStatus(), task == null ? null : task.getDueAt(),
                instance.getLastOperatedAt() == null ? instance.getStartedAt() : instance.getLastOperatedAt(),
                task == null ? null : task.getAssignmentKind(), task == null ? null : task.getOriginalAssigneeId(),
                title(task == null ? null : task.getOriginalAssigneeId(), userTitles),
                task == null ? null : task.getDelegatedFromUserId(),
                title(task == null ? null : task.getDelegatedFromUserId(), userTitles),
                task == null ? null : firstText(task.getDelegatedToUserId(), task.getAssigneeId()),
                title(task == null ? null : firstText(task.getDelegatedToUserId(), task.getAssigneeId()),
                        userTitles),
                task == null ? null : task.getPrincipalCanProcess(),
                noticeReadStatus(task), noticeSourceType(task), delegationTaskCount,
                Boolean.TRUE.equals(node == null ? null : node.getAddedByAddSign()),
                addSignSourceNodeKey(node), addSignOperatorId(node), title(addSignOperatorId(node), userTitles),
                addSignAt(node));
    }

    private String addSignSourceNodeKey(WorkflowNodeInstance node) {
        return node != null && Boolean.TRUE.equals(node.getAddedByAddSign())
                ? blankToNull(node.getAddSignSourceNodeKey())
                : null;
    }

    private String addSignOperatorId(WorkflowNodeInstance node) {
        return node != null && Boolean.TRUE.equals(node.getAddedByAddSign())
                ? blankToNull(node.getAddSignOperatorId())
                : null;
    }

    private Map<String, String> userTitles(WorkflowTask task) {
        return userTitles(task == null ? List.of() : List.of(task));
    }

    private Map<String, String> userTitles(List<WorkflowTask> tasks) {
        LinkedHashSet<String> userIds = new LinkedHashSet<>();
        for (WorkflowTask task : tasks == null ? List.<WorkflowTask>of() : tasks) {
            addUserId(userIds, task.getAssigneeId());
            addUserId(userIds, task.getOriginalAssigneeId());
            addUserId(userIds, task.getActualProcessorId());
            addUserId(userIds, task.getDelegatedFromUserId());
            addUserId(userIds, task.getDelegatedToUserId());
            addUserId(userIds, task.getTransferredFromUserId());
            addUserId(userIds, task.getTransferredBy());
        }
        Map<String, String> titles = userTitleResolver.titles(userIds);
        return titles == null ? Map.of() : titles;
    }

    private void addUserId(Set<String> userIds, String userId) {
        if (userId != null && !userId.isBlank()) {
            userIds.add(userId);
        }
    }

    private List<String> titles(List<String> userIds, Map<String, String> userTitles) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        Map<String, String> titles = userTitles == null ? Map.of() : userTitles;
        return userIds.stream()
                .map(titles::get)
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private String title(String userId, Map<String, String> userTitles) {
        return userId == null || userTitles == null ? null : userTitles.get(userId);
    }

    private String nodeTitle(WorkflowNodeInstance node) {
        return node == null ? null : firstText(node.getNodeTitle(), node.getNodeKey());
    }

    private Instant addSignAt(WorkflowNodeInstance node) {
        return node != null && Boolean.TRUE.equals(node.getAddedByAddSign()) ? node.getAddSignAt() : null;
    }

    private String noticeSourceType(WorkflowTask task) {
        if (task == null || task.getTaskKind() != WorkflowTaskKind.NOTICE) {
            return null;
        }
        if (isDelegationCompletionNotice(task)) {
            return "DELEGATION_COMPLETED";
        }
        return null;
    }

    private boolean isDelegationCompletionNotice(WorkflowTask task) {
        if (task == null || task.getTaskKind() != WorkflowTaskKind.NOTICE) {
            return false;
        }
        if (hasText(task.getDelegatedFromUserId())
                && hasText(task.getActualProcessorId())
                && !task.getActualProcessorId().equals(task.getDelegatedFromUserId())
                && (hasText(task.getDelegatedToUserId())
                || hasText(task.getDelegationPolicyId())
                || task.getAssignmentKind() == WorkflowAssignmentKind.DELEGATED
                || task.getAssignmentKind() == WorkflowAssignmentKind.TRANSFERRED)) {
            return true;
        }
        String snapshot = task.getAssignmentSnapshotText();
        return snapshot != null && snapshot.contains("DELEGATION_COMPLETED");
    }

    private WorkflowTaskAvailableAction enrich(WorkflowTaskAvailableAction action, WorkflowTask task,
                                               WorkflowNodeInstance node) {
        WorkflowTaskAvailableAction enriched = action.withTask(task, node);
        if ("reject".equals(action.actionCode())) {
            List<String> modes = action.rejectReturnToMeSupported()
                    ? List.of(WorkflowRejectResubmitMode.RESTART.getCode(),
                    WorkflowRejectResubmitMode.RETURN_TO_ME.getCode())
                    : List.of(WorkflowRejectResubmitMode.RESTART.getCode());
            enriched = enriched.withRejectResubmitModes(modes, WorkflowRejectResubmitMode.RESTART.getCode());
        }
        return enriched;
    }

    private Map<String, WorkflowNodeInstance> nodeById(String instanceId) {
        Map<String, WorkflowNodeInstance> values = new LinkedHashMap<>();
        nodeDao.query(Criteria.of().eq("instanceId", instanceId), ALL, Sort.asc("createdAt"))
                .forEach(node -> values.put(node.getId(), node));
        return values;
    }

    private List<String> currentAssignees(String instanceId) {
        return taskDao.query(Criteria.of()
                        .eq("instanceId", instanceId)
                        .eq("taskStatus", WorkflowTaskStatus.TODO),
                ALL, Sort.asc("createdAt"))
                .stream()
                .flatMap(task -> assigneeIds(task).stream())
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private WorkflowNodeInstance currentAddSignNode(WorkflowInstance instance) {
        if (instance == null || instance.getCurrentNodeKeys() == null || instance.getCurrentNodeKeys().isBlank()) {
            return null;
        }
        Map<String, WorkflowNodeInstance> addSignNodesByKey = safeNodes(instance.getId()).stream()
                .filter(node -> Boolean.TRUE.equals(node.getAddedByAddSign()))
                .filter(node -> node.getNodeKey() != null)
                .collect(Collectors.toMap(WorkflowNodeInstance::getNodeKey, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
        if (addSignNodesByKey.isEmpty()) {
            return null;
        }
        for (String nodeKey : instance.getCurrentNodeKeys().split("[,;\\s]+")) {
            WorkflowNodeInstance node = addSignNodesByKey.get(nodeKey);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    private List<WorkflowNodeInstance> safeNodes(String instanceId) {
        List<WorkflowNodeInstance> nodes = nodeDao.query(Criteria.of().eq("instanceId", instanceId),
                ALL, Sort.asc("createdAt"));
        return nodes == null ? List.of() : nodes;
    }

    private WorkflowInstance requireInstance(String instanceId) {
        String validInstanceId = requireText(instanceId, "workflow instance id must not be blank");
        WorkflowInstance instance = instanceDao.findById(validInstanceId);
        if (instance == null) {
            throw new PlatformException("workflow instance not found: " + validInstanceId);
        }
        return instance;
    }

    private String requireOperator(String operatorId) {
        if (operatorId != null && !operatorId.isBlank()) {
            return operatorId;
        }
        return CurrentUserContext.currentUser()
                .map(user -> user.userId())
                .filter(userId -> !userId.isBlank())
                .orElseThrow(() -> new PlatformException("workflow operator id must not be blank"));
    }

    private PageRequest page(PageRequest pageRequest) {
        return pageRequest == null ? PageRequest.of(1, 20) : pageRequest;
    }

    private boolean matches(WorkflowWorkbenchCard card, WorkflowWorkbenchQueryRequest request) {
        WorkflowWorkbenchQueryRequest normalized = request == null ? WorkflowWorkbenchQueryRequest.empty() : request;
        if (!sameText(normalized.moduleAlias(), card.moduleAlias())) {
            return false;
        }
        if (!sameText(normalized.recordId(), card.recordId())) {
            return false;
        }
        if (!sameText(normalized.definitionId(), card.definitionId())) {
            return false;
        }
        if (!sameText(normalized.effectiveWorkflowVersionId(), card.workflowVersionId())) {
            return false;
        }
        if (!same(normalized.instanceStatus(), card.instanceStatus())
                || !same(normalized.taskKind(), card.taskKind())
                || !same(normalized.taskStatus(), card.taskStatus())
                || !same(normalized.assignmentKind(), card.assignmentKind())
                || !same(normalized.overtimeStatus(), card.overtimeStatus())) {
            return false;
        }
        if (!matchesReadStatus(normalized.readStatus(), card.readStatus())) {
            return false;
        }
        if (!same(normalized.addedByAddSign(), card.addedByAddSign())) {
            return false;
        }
        if (!sameText(normalized.addSignSourceNodeKey(), card.addSignSourceNodeKey())) {
            return false;
        }
        if (!matchesNodeKey(normalized.nodeKey(), card)) {
            return false;
        }
        return inRange(card.startedAt(), normalized.startedFrom(), normalized.startedTo())
                && inRange(card.receivedAt(), normalized.receivedFrom(), normalized.receivedTo())
                && inRange(card.completedAt(), normalized.completedFrom(), normalized.completedTo())
                && inRange(card.lastOperatedAt(), normalized.lastOperatedFrom(), normalized.lastOperatedTo())
                && inRange(card.dueAt(), normalized.dueFrom(), normalized.dueTo());
    }

    private WorkflowWorkbenchQueryRequest filtersOnly(WorkflowWorkbenchQueryRequest request) {
        WorkflowWorkbenchQueryRequest normalized = request == null ? WorkflowWorkbenchQueryRequest.empty() : request;
        return new WorkflowWorkbenchQueryRequest(normalized.moduleAlias(), normalized.recordId(),
                normalized.definitionId(), normalized.workflowVersionId(), normalized.definitionVersionId(),
                normalized.instanceStatus(), normalized.nodeKey(), normalized.taskKind(), normalized.taskStatus(),
                normalized.assignmentKind(), normalized.overtimeStatus(), normalized.readStatus(),
                normalized.startedFrom(), normalized.startedTo(), normalized.receivedFrom(), normalized.receivedTo(),
                normalized.completedFrom(), normalized.completedTo(), normalized.lastOperatedFrom(),
                normalized.lastOperatedTo(), normalized.dueFrom(), normalized.dueTo(), normalized.addedByAddSign(),
                normalized.addSignSourceNodeKey(), List.of());
    }

    private Comparator<WorkflowWorkbenchCard> sorter(String boardType, WorkflowWorkbenchQueryRequest request) {
        WorkflowWorkbenchQueryRequest normalized = request == null ? WorkflowWorkbenchQueryRequest.empty() : request;
        List<WorkflowWorkbenchSort> sorts = normalized.sorts().isEmpty() ? defaultSorts(boardType) : normalized.sorts();
        Comparator<WorkflowWorkbenchCard> comparator = null;
        for (WorkflowWorkbenchSort sort : sorts) {
            String field = requireText(sort == null ? null : sort.field(),
                    "workflow workbench sort field must not be blank");
            validateSortField(field);
            WorkflowSortDirection direction = sort.direction() == null ? WorkflowSortDirection.DESC : sort.direction();
            Comparator<WorkflowWorkbenchCard> fieldComparator = (left, right) ->
                    compareSortValue(sortValue(left, field), sortValue(right, field), direction);
            comparator = comparator == null ? fieldComparator : comparator.thenComparing(fieldComparator);
        }
        return comparator == null ? (left, right) -> 0 : comparator;
    }

    private void validateSortField(String field) {
        switch (field) {
            case "moduleAlias", "recordId", "definitionId", "workflowVersionId", "definitionVersionId",
                    "instanceStatus", "approvalStatus", "nodeKey", "taskKind", "taskStatus", "actionCode",
                    "assignmentKind", "overtimeStatus", "readStatus", "startedAt", "receivedAt", "completedAt",
                    "lastOperatedAt", "dueAt", "addSignAt", "addedByAddSign", "addSignSourceNodeKey",
                    "addSignOperatorId", "originalAssigneeId", "delegatedFromUserId", "delegatedToUserId",
                    "principalCanProcess",
                    "delegationTaskCount" -> {
            }
            default -> throw new PlatformException("unsupported workflow workbench sort field: " + field);
        }
    }

    private List<WorkflowWorkbenchSort> defaultSorts(String boardType) {
        return switch (boardType) {
            case "TODO" -> List.of(new WorkflowWorkbenchSort("dueAt", WorkflowSortDirection.ASC),
                    new WorkflowWorkbenchSort("receivedAt", WorkflowSortDirection.DESC));
            case "DONE" -> List.of(new WorkflowWorkbenchSort("completedAt", WorkflowSortDirection.DESC),
                    new WorkflowWorkbenchSort("lastOperatedAt", WorkflowSortDirection.DESC));
            case "NOTICE" -> List.of(new WorkflowWorkbenchSort("receivedAt", WorkflowSortDirection.DESC));
            case "TRACKING" -> List.of(new WorkflowWorkbenchSort("startedAt", WorkflowSortDirection.DESC),
                    new WorkflowWorkbenchSort("lastOperatedAt", WorkflowSortDirection.DESC));
            case "DELEGATION" -> List.of(new WorkflowWorkbenchSort("dueAt", WorkflowSortDirection.ASC),
                    new WorkflowWorkbenchSort("receivedAt", WorkflowSortDirection.DESC));
            default -> List.of(new WorkflowWorkbenchSort("lastOperatedAt", WorkflowSortDirection.DESC));
        };
    }

    private Comparable<?> sortValue(WorkflowWorkbenchCard card, String field) {
        return switch (field) {
            case "moduleAlias" -> card.moduleAlias();
            case "recordId" -> card.recordId();
            case "definitionId" -> card.definitionId();
            case "workflowVersionId", "definitionVersionId" -> card.workflowVersionId();
            case "instanceStatus" -> card.instanceStatus();
            case "approvalStatus" -> card.approvalStatus();
            case "nodeKey" -> card.nodeKey();
            case "taskKind" -> card.taskKind();
            case "taskStatus" -> card.taskStatus();
            case "actionCode" -> card.actionCode();
            case "assignmentKind" -> card.assignmentKind();
            case "overtimeStatus" -> card.overtimeStatus();
            case "readStatus" -> card.readStatus();
            case "startedAt" -> card.startedAt();
            case "receivedAt" -> card.receivedAt();
            case "completedAt" -> card.completedAt();
            case "lastOperatedAt" -> card.lastOperatedAt();
            case "dueAt" -> card.dueAt();
            case "addSignAt" -> card.addSignAt();
            case "addedByAddSign" -> card.addedByAddSign();
            case "addSignSourceNodeKey" -> card.addSignSourceNodeKey();
            case "addSignOperatorId" -> card.addSignOperatorId();
            case "originalAssigneeId" -> card.originalAssigneeId();
            case "delegatedFromUserId" -> card.delegatedFromUserId();
            case "delegatedToUserId" -> card.delegatedToUserId();
            case "principalCanProcess" -> card.principalCanProcess();
            case "delegationTaskCount" -> card.delegationTaskCount();
            default -> throw new PlatformException("unsupported workflow workbench sort field: " + field);
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private int compareSortValue(Comparable left, Comparable right, WorkflowSortDirection direction) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        int compared = left.compareTo(right);
        return direction == WorkflowSortDirection.ASC ? compared : -compared;
    }

    private boolean sameText(String expected, String actual) {
        return expected == null || expected.isBlank() || expected.equals(actual);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean same(Object expected, Object actual) {
        return expected == null || expected.equals(actual);
    }

    private boolean matchesNodeKey(String expected, WorkflowWorkbenchCard card) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        if (expected.equals(card.nodeKey())) {
            return true;
        }
        String currentNodeKeys = card.currentNodeKeys();
        if (currentNodeKeys == null || currentNodeKeys.isBlank()) {
            return false;
        }
        for (String nodeKey : currentNodeKeys.split("[,;\\s]+")) {
            if (expected.equals(nodeKey)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesReadStatus(WorkflowNoticeReadStatus expected, WorkflowNoticeReadStatus actual) {
        return expected == null || expected == WorkflowNoticeReadStatus.ALL || expected == actual;
    }

    private WorkflowNoticeReadStatus noticeReadStatus(WorkflowTask task) {
        if (task == null || task.getTaskKind() != WorkflowTaskKind.NOTICE) {
            return null;
        }
        if (task.getTaskStatus() == WorkflowTaskStatus.NOTICED) {
            return WorkflowNoticeReadStatus.READ;
        }
        if (task.getTaskStatus() == WorkflowTaskStatus.TODO) {
            return WorkflowNoticeReadStatus.UNREAD;
        }
        return null;
    }

    private String actionCode(WorkflowTask task) {
        if (task == null) {
            return null;
        }
        if (task.getDecision() != null && !task.getDecision().isBlank()) {
            return task.getDecision();
        }
        return task.getTaskStatus() == WorkflowTaskStatus.TRANSFERRED ? "transfer" : null;
    }

    private WorkflowOvertimeStatus overtimeStatus(WorkflowWorkbenchCard card) {
        return card.overtimeStatus() == null ? WorkflowOvertimeStatus.NORMAL : card.overtimeStatus();
    }

    private String doneStatCode(WorkflowWorkbenchCard card) {
        if (card.taskStatus() == WorkflowTaskStatus.REJECTED) {
            return "REJECTED";
        }
        if (card.taskStatus() == WorkflowTaskStatus.ROLLED_BACK) {
            return "ROLLED_BACK";
        }
        if (card.taskStatus() == WorkflowTaskStatus.TRANSFERRED) {
            return "TRANSFERRED";
        }
        return "DONE";
    }

    private <E extends Enum<E> & CodeTitleEnum> List<WorkflowWorkbenchStatItem> statsWithAll(long all,
                                                                                            E[] values,
                                                                                            Map<E, Long> counts) {
        java.util.ArrayList<WorkflowWorkbenchStatItem> items = new java.util.ArrayList<>();
        items.add(stat("ALL", "全部", all));
        for (E value : values) {
            items.add(stat(value, counts.getOrDefault(value, 0L)));
        }
        return List.copyOf(items);
    }

    private WorkflowWorkbenchStatItem stat(CodeTitleEnum value, long count) {
        return stat(((Enum<?>) value).name(), value.getTitle(), count);
    }

    private WorkflowWorkbenchStatItem stat(String code, String label, long count) {
        return new WorkflowWorkbenchStatItem(code, label, count);
    }

    private boolean inRange(Comparable<?> value, Comparable<?> from, Comparable<?> to) {
        if (from == null && to == null) {
            return true;
        }
        if (value == null) {
            return false;
        }
        return (from == null || compareRange(value, from) >= 0)
                && (to == null || compareRange(value, to) < 0);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private int compareRange(Comparable value, Comparable bound) {
        return value.compareTo(bound);
    }

    private <T> List<T> pageItems(List<T> items, PageRequest pageRequest) {
        int from = Math.min(pageRequest.getOffset(), items.size());
        int to = Math.min(from + pageRequest.getLimit(), items.size());
        return items.subList(from, to);
    }

    private String firstText(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(message);
        }
        return value;
    }
}
