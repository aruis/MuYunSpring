package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowRuntimeReadFacade {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final WorkflowInstanceDao instanceDao;
    private final WorkflowTaskDao taskDao;
    private final WorkflowNodeInstanceDao nodeDao;
    private final WorkflowRouteInstanceDao routeDao;
    private final WorkflowEventDao eventDao;
    private final WorkflowTaskActionAvailabilityService availabilityService;
    private final WorkflowActionPolicyService actionPolicyService;

    public WorkflowRuntimeReadFacade(WorkflowInstanceDao instanceDao,
                                     WorkflowTaskDao taskDao,
                                     WorkflowNodeInstanceDao nodeDao,
                                     WorkflowRouteInstanceDao routeDao,
                                     WorkflowEventDao eventDao,
                                     WorkflowTaskActionAvailabilityService availabilityService) {
        this(instanceDao, taskDao, nodeDao, routeDao, eventDao, availabilityService,
                new WorkflowActionPolicyService());
    }

    @Autowired
    public WorkflowRuntimeReadFacade(WorkflowInstanceDao instanceDao,
                                     WorkflowTaskDao taskDao,
                                     WorkflowNodeInstanceDao nodeDao,
                                     WorkflowRouteInstanceDao routeDao,
                                     WorkflowEventDao eventDao,
                                     WorkflowTaskActionAvailabilityService availabilityService,
                                     WorkflowActionPolicyService actionPolicyService) {
        this.instanceDao = instanceDao;
        this.taskDao = taskDao;
        this.nodeDao = nodeDao;
        this.routeDao = routeDao;
        this.eventDao = eventDao;
        this.availabilityService = availabilityService;
        this.actionPolicyService = actionPolicyService == null
                ? new WorkflowActionPolicyService()
                : actionPolicyService;
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

    public List<WorkflowTaskAvailableAction> instanceAvailableActions(String instanceId, String operatorId) {
        WorkflowInstance instance = requireInstance(instanceId);
        actionPolicyService.requireRecordView(instance);
        String validOperatorId = requireOperator(operatorId);
        Map<String, WorkflowNodeInstance> nodes = nodeById(instance.getId());
        return taskDao.query(Criteria.of().eq("instanceId", instance.getId()), ALL, Sort.asc("createdAt"))
                .stream()
                .filter(task -> task.getTaskStatus() == WorkflowTaskStatus.TODO)
                .filter(task -> canOperate(task, validOperatorId))
                .flatMap(task -> availabilityService.availableActions(task.getId(), validOperatorId).stream()
                        .map(action -> enrich(action, task, nodes.get(task.getNodeInstanceId()))))
                .toList();
    }

    public List<WorkflowWorkbenchCard> todoCards(String assigneeId, PageRequest pageRequest) {
        String validAssigneeId = requireText(assigneeId, "workflow assignee id must not be blank");
        List<WorkflowTask> tasks = taskDao.query(Criteria.of()
                        .eq("assigneeId", validAssigneeId)
                        .eq("taskStatus", WorkflowTaskStatus.TODO)
                        .in("taskKind", List.of(WorkflowTaskKind.APPROVAL, WorkflowTaskKind.BUSINESS,
                                WorkflowTaskKind.RESUBMIT)),
                page(pageRequest), Sort.asc("dueAt"), Sort.desc("createdAt"));
        return cards("TODO", tasks);
    }

    public List<WorkflowWorkbenchCard> doneCards(String processorId, PageRequest pageRequest) {
        String validProcessorId = requireText(processorId, "workflow processor id must not be blank");
        List<WorkflowTask> tasks = taskDao.query(Criteria.of()
                        .eq("actualProcessorId", validProcessorId)
                        .in("taskStatus", List.of(WorkflowTaskStatus.DONE, WorkflowTaskStatus.REJECTED,
                                WorkflowTaskStatus.ROLLED_BACK, WorkflowTaskStatus.TRANSFERRED)),
                page(pageRequest), Sort.desc("completedAt"), Sort.desc("updatedAt"));
        return cards("DONE", tasks);
    }

    public List<WorkflowWorkbenchCard> noticeCards(String assigneeId, PageRequest pageRequest) {
        String validAssigneeId = requireText(assigneeId, "workflow assignee id must not be blank");
        List<WorkflowTask> tasks = taskDao.query(Criteria.of()
                        .eq("assigneeId", validAssigneeId)
                        .eq("taskKind", WorkflowTaskKind.NOTICE)
                        .in("taskStatus", List.of(WorkflowTaskStatus.TODO, WorkflowTaskStatus.NOTICED)),
                page(pageRequest), Sort.desc("createdAt"));
        return cards("NOTICE", tasks);
    }

    public List<WorkflowWorkbenchCard> trackingCards(String starterId, PageRequest pageRequest) {
        String validStarterId = requireText(starterId, "workflow starter id must not be blank");
        List<WorkflowInstance> instances = instanceDao.query(Criteria.of().eq("startedBy", validStarterId),
                page(pageRequest), Sort.desc("startedAt"), Sort.desc("updatedAt"));
        return instances.stream()
                .map(instance -> card("TRACKING", instance, null, null, currentAssignees(instance.getId())))
                .toList();
    }

    private List<WorkflowWorkbenchCard> cards(String boardType, List<WorkflowTask> tasks) {
        Map<String, WorkflowInstance> instances = new LinkedHashMap<>();
        Map<String, WorkflowNodeInstance> nodes = new LinkedHashMap<>();
        for (WorkflowTask task : tasks) {
            instances.computeIfAbsent(task.getInstanceId(), instanceDao::findById);
            if (task.getNodeInstanceId() != null) {
                nodes.computeIfAbsent(task.getNodeInstanceId(), nodeDao::findById);
            }
        }
        return tasks.stream()
                .map(task -> card(boardType, instances.get(task.getInstanceId()), task,
                        nodes.get(task.getNodeInstanceId()), assigneeIds(task)))
                .sorted(Comparator.comparing(WorkflowWorkbenchCard::lastOperatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<String> assigneeIds(WorkflowTask task) {
        if (task == null || task.getAssigneeId() == null || task.getAssigneeId().isBlank()) {
            return List.of();
        }
        return List.of(task.getAssigneeId());
    }

    private WorkflowWorkbenchCard card(String boardType,
                                       WorkflowInstance instance,
                                       WorkflowTask task,
                                       WorkflowNodeInstance node,
                                       List<String> currentAssigneeIds) {
        if (instance == null) {
            throw new PlatformException("workflow instance not found: " + (task == null ? null : task.getInstanceId()));
        }
        return new WorkflowWorkbenchCard(boardType, instance.getId(), instance.getModuleAlias(), instance.getRecordId(),
                instance.getInstanceStatus(), instance.getApprovalStatus(), task == null ? null : task.getId(),
                task == null ? null : task.getTaskKind(), task == null ? null : task.getTaskStatus(),
                node == null ? null : node.getNodeKey(), instance.getCurrentNodeKeys(), currentAssigneeIds,
                task == null ? instance.getStartedAt() : task.getCreatedAt(),
                task == null ? instance.getCompletedAt() : task.getCompletedAt(),
                task == null ? instance.getLastActionCode() : task.getDecision(),
                node == null ? null : node.getOvertimeStatus(),
                instance.getLastOperatedAt() == null ? instance.getStartedAt() : instance.getLastOperatedAt(),
                task == null ? null : task.getAssignmentKind(), task == null ? null : task.getOriginalAssigneeId(),
                task == null ? null : task.getDelegatedFromUserId(),
                task == null ? null : task.getAssigneeId(), null);
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
                .map(WorkflowTask::getAssigneeId)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private WorkflowInstance requireInstance(String instanceId) {
        String validInstanceId = requireText(instanceId, "workflow instance id must not be blank");
        WorkflowInstance instance = instanceDao.findById(validInstanceId);
        if (instance == null) {
            throw new PlatformException("workflow instance not found: " + validInstanceId);
        }
        return instance;
    }

    private boolean canOperate(WorkflowTask task, String operatorId) {
        return operatorId.equals(task.getAssigneeId());
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

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(message);
        }
        return value;
    }
}
