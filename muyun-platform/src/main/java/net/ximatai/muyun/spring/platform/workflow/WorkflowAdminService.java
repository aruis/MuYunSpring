package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class WorkflowAdminService {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final WorkflowInstanceDao instanceDao;
    private final WorkflowTaskDao taskDao;
    private final WorkflowNodeInstanceDao nodeInstanceDao;
    private final WorkflowRouteInstanceDao routeInstanceDao;
    private final WorkflowEventDao eventDao;
    private final WorkflowActionPolicyService actionPolicyService;
    private final WorkflowInstanceActionService instanceActionService;
    private final WorkflowTaskActionService taskActionService;
    private final WorkflowHistoryQueryService historyQueryService;

    public WorkflowAdminService(WorkflowInstanceDao instanceDao,
                                WorkflowTaskDao taskDao,
                                WorkflowActionPolicyService actionPolicyService,
                                WorkflowInstanceActionService instanceActionService,
                                WorkflowTaskActionService taskActionService) {
        this(instanceDao, taskDao, null, null, null, actionPolicyService, instanceActionService, taskActionService, null);
    }

    public WorkflowAdminService(WorkflowInstanceDao instanceDao,
                                WorkflowTaskDao taskDao,
                                WorkflowActionPolicyService actionPolicyService,
                                WorkflowInstanceActionService instanceActionService,
                                WorkflowTaskActionService taskActionService,
                                WorkflowHistoryQueryService historyQueryService) {
        this(instanceDao, taskDao, null, null, null, actionPolicyService, instanceActionService, taskActionService,
                historyQueryService);
    }

    public WorkflowAdminService(WorkflowInstanceDao instanceDao,
                                WorkflowTaskDao taskDao,
                                WorkflowNodeInstanceDao nodeInstanceDao,
                                WorkflowActionPolicyService actionPolicyService,
                                WorkflowInstanceActionService instanceActionService,
                                WorkflowTaskActionService taskActionService,
                                WorkflowHistoryQueryService historyQueryService) {
        this(instanceDao, taskDao, nodeInstanceDao, null, null, actionPolicyService, instanceActionService,
                taskActionService, historyQueryService);
    }

    @Autowired
    public WorkflowAdminService(WorkflowInstanceDao instanceDao,
                                WorkflowTaskDao taskDao,
                                WorkflowNodeInstanceDao nodeInstanceDao,
                                WorkflowRouteInstanceDao routeInstanceDao,
                                WorkflowEventDao eventDao,
                                WorkflowActionPolicyService actionPolicyService,
                                WorkflowInstanceActionService instanceActionService,
                                WorkflowTaskActionService taskActionService,
                                WorkflowHistoryQueryService historyQueryService) {
        this.instanceDao = instanceDao;
        this.taskDao = taskDao;
        this.nodeInstanceDao = nodeInstanceDao;
        this.routeInstanceDao = routeInstanceDao;
        this.eventDao = eventDao;
        this.actionPolicyService = actionPolicyService == null ? new WorkflowActionPolicyService() : actionPolicyService;
        this.instanceActionService = instanceActionService;
        this.taskActionService = taskActionService;
        this.historyQueryService = historyQueryService;
    }

    public List<WorkflowAdminInstanceView> queryCurrentInstances(WorkflowAdminInstanceQueryRequest request,
                                                                 PageRequest pageRequest) {
        actionPolicyService.requireManagementAction(WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION);
        WorkflowAdminInstanceQueryRequest normalized = request == null
                ? WorkflowAdminInstanceQueryRequest.empty()
                : request;
        Criteria criteria = Criteria.of()
                .eq("instanceStatus", normalized.instanceStatus() == null
                        ? WorkflowInstanceStatus.RUNNING
                        : normalized.instanceStatus());
        if (hasText(normalized.moduleAlias())) {
            criteria.eq("moduleAlias", normalized.moduleAlias());
        }
        if (hasText(normalized.recordId())) {
            criteria.eq("recordId", normalized.recordId());
        }
        if (hasText(normalized.starterId())) {
            criteria.eq("startedBy", normalized.starterId());
        }
        if (normalized.approvalStatus() != null) {
            criteria.eq("approvalStatus", normalized.approvalStatus());
        }
        List<WorkflowAdminInstanceView> views = instanceDao.query(criteria, ALL,
                        Sort.desc("lastOperatedAt"), Sort.desc("startedAt"), Sort.desc("updatedAt"))
                .stream()
                .map(this::toInstanceView)
                .filter(view -> matchesCurrentAssignee(view, normalized.currentAssigneeId()))
                .filter(view -> matchesOvertime(view, normalized.overtimeStatus()))
                .toList();
        return pageItems(views, page(pageRequest));
    }

    public List<WorkflowTask> currentTodoTasks(String instanceId) {
        WorkflowInstance instance = requireRunningInstance(instanceId);
        actionPolicyService.requireManagementAction(WorkflowActionPolicyService.MANAGEMENT_TODO_TASK_QUERY_ACTION);
        return taskDao.query(Criteria.of()
                        .eq("instanceId", instance.getId())
                        .eq("taskStatus", WorkflowTaskStatus.TODO),
                ALL, Sort.asc("createdAt"));
    }

    public List<WorkflowAdminActiveTaskView> currentTodoTaskViews(String instanceId) {
        WorkflowInstance instance = requireRunningInstance(instanceId);
        actionPolicyService.requireManagementAction(WorkflowActionPolicyService.MANAGEMENT_FORCE_APPROVE_ACTION);
        return taskDao.query(Criteria.of()
                        .eq("instanceId", instance.getId())
                        .eq("taskKind", WorkflowTaskKind.APPROVAL)
                        .eq("taskStatus", WorkflowTaskStatus.TODO),
                ALL, Sort.asc("createdAt")).stream()
                .filter(task -> task.getTaskKind() == WorkflowTaskKind.APPROVAL)
                .filter(task -> task.getTaskStatus() == WorkflowTaskStatus.TODO)
                .map(this::toActiveTaskView)
                .filter(view -> view != null)
                .toList();
    }

    @Transactional
    public WorkflowInstanceActionResult reset(WorkflowInstanceActionRequest request) {
        return instanceActionService.managementReset(request);
    }

    @Transactional
    public WorkflowInstanceActionResult forceTerminate(WorkflowInstanceActionRequest request) {
        return instanceActionService.forceTerminate(request);
    }

    @Transactional
    public WorkflowTaskActionResult forceApprove(WorkflowTaskActionRequest request) {
        return taskActionService.forceApprove(request);
    }

    public WorkflowRuntimeRenderBundle renderCurrentBundle(String instanceId) {
        WorkflowInstance instance = requireRunningInstance(instanceId);
        actionPolicyService.requireManagementAction(WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION);
        List<WorkflowNodeInstance> nodes = nodeInstanceDao.query(Criteria.of().eq("instanceId", instance.getId()),
                ALL, Sort.asc("createdAt"));
        List<WorkflowRouteInstance> routes = routeInstanceDao.query(Criteria.of().eq("instanceId", instance.getId()),
                ALL, Sort.asc("createdAt"));
        return new WorkflowRuntimeRenderBundle("RUNTIME", instance, nodes, routes);
    }

    public WorkflowRuntimeRenderBundle renderInstanceBundle(String instanceId) {
        return renderCurrentBundle(instanceId);
    }

    public List<WorkflowEvent> currentEvents(String instanceId) {
        WorkflowInstance instance = requireRunningInstance(instanceId);
        actionPolicyService.requireManagementAction(WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION);
        return eventDao.query(Criteria.of().eq("instanceId", instance.getId()), ALL,
                Sort.asc("occurredAt"), Sort.asc("createdAt"));
    }

    public List<WorkflowTask> currentTasks(String instanceId) {
        WorkflowInstance instance = requireRunningInstance(instanceId);
        actionPolicyService.requireManagementAction(WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION);
        return taskDao.query(Criteria.of().eq("instanceId", instance.getId()), ALL, Sort.asc("createdAt"));
    }

    public List<WorkflowHistoryInstance> queryHistory(String moduleAlias, String recordId, PageRequest pageRequest) {
        return historyQueryService.queryAdminHistory(moduleAlias, recordId, pageRequest);
    }

    public WorkflowRuntimeRenderBundle renderHistoryBundle(String historyInstanceId) {
        return historyQueryService.renderAdminBundle(historyInstanceId);
    }

    public List<WorkflowEvent> historyEvents(String historyInstanceId) {
        return historyQueryService.adminEvents(historyInstanceId);
    }

    public List<WorkflowHistoryEventView> historyEventViews(String historyInstanceId) {
        return historyQueryService.adminEventViews(historyInstanceId);
    }

    @Transactional
    public int deleteHistory(String historyInstanceId) {
        return historyQueryService.deleteHistory(historyInstanceId);
    }

    private WorkflowAdminActiveTaskView toActiveTaskView(WorkflowTask task) {
        if (task == null || task.getNodeInstanceId() == null || task.getNodeInstanceId().isBlank()) {
            return null;
        }
        WorkflowNodeInstance node = nodeInstanceDao == null ? null : nodeInstanceDao.findById(task.getNodeInstanceId());
        if (node == null
                || node.getNodeStatus() != WorkflowNodeStatus.ACTIVE
                || node.getNodeType() != WorkflowNodeType.APPROVAL) {
            return null;
        }
        return WorkflowAdminActiveTaskView.from(task, node);
    }

    private WorkflowAdminInstanceView toInstanceView(WorkflowInstance instance) {
        List<WorkflowTask> todoTasks = taskDao.query(Criteria.of()
                        .eq("instanceId", instance.getId())
                        .eq("taskStatus", WorkflowTaskStatus.TODO),
                ALL, Sort.asc("createdAt"));
        List<WorkflowNodeInstance> activeNodes = nodeInstanceDao.query(Criteria.of()
                        .eq("instanceId", instance.getId())
                        .eq("nodeStatus", WorkflowNodeStatus.ACTIVE),
                ALL, Sort.asc("createdAt"));
        List<String> activeNodeKeys = activeNodes.stream()
                .map(WorkflowNodeInstance::getNodeKey)
                .filter(this::hasText)
                .distinct()
                .toList();
        List<String> currentTaskIds = todoTasks.stream()
                .map(WorkflowTask::getId)
                .filter(this::hasText)
                .toList();
        List<String> currentAssigneeIds = todoTasks.stream()
                .flatMap(task -> assigneeIds(task).stream())
                .filter(this::hasText)
                .distinct()
                .toList();
        return new WorkflowAdminInstanceView(
                instance.getId(),
                instance.getModuleAlias(),
                instance.getRecordId(),
                instance.getDefinitionId(),
                instance.getWorkflowVersionId(),
                instance.getVersionNo(),
                instance.getInstanceStatus(),
                instance.getApprovalStatus(),
                instance.getStartedBy(),
                instance.getStartedAt(),
                activeNodeKeys,
                currentTaskIds,
                currentAssigneeIds,
                aggregateOvertimeStatus(activeNodes),
                instance.getUpdatedAt(),
                instance.getLastOperatedAt());
    }

    private WorkflowOvertimeStatus aggregateOvertimeStatus(List<WorkflowNodeInstance> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return WorkflowOvertimeStatus.NORMAL;
        }
        return nodes.stream()
                .map(WorkflowNodeInstance::getOvertimeStatus)
                .filter(status -> status != null)
                .max(Comparator.comparingInt(this::overtimeSeverity))
                .orElse(WorkflowOvertimeStatus.NORMAL);
    }

    private int overtimeSeverity(WorkflowOvertimeStatus status) {
        return switch (status) {
            case OVERDUE -> 3;
            case WARNED -> 2;
            case NORMAL -> 1;
        };
    }

    private List<String> assigneeIds(WorkflowTask task) {
        if (task == null || !hasText(task.getAssigneeId())) {
            return List.of();
        }
        if (task.getAssignmentKind() == WorkflowAssignmentKind.DELEGATED
                && Boolean.TRUE.equals(task.getPrincipalCanProcess())
                && hasText(task.getDelegatedFromUserId())
                && task.getTransferredFromUserId() == null) {
            return List.of(task.getAssigneeId(), task.getDelegatedFromUserId());
        }
        return List.of(task.getAssigneeId());
    }

    private boolean matchesCurrentAssignee(WorkflowAdminInstanceView view, String currentAssigneeId) {
        return !hasText(currentAssigneeId) || view.currentAssigneeIds().contains(currentAssigneeId);
    }

    private boolean matchesOvertime(WorkflowAdminInstanceView view, WorkflowOvertimeStatus overtimeStatus) {
        return overtimeStatus == null || overtimeStatus == view.overtimeStatus();
    }

    private <T> List<T> pageItems(List<T> items, PageRequest pageRequest) {
        PageRequest normalized = page(pageRequest);
        int from = Math.min(normalized.getOffset(), items.size());
        int to = Math.min(from + normalized.getLimit(), items.size());
        return items.subList(from, to);
    }

    private PageRequest page(PageRequest pageRequest) {
        return pageRequest == null ? PageRequest.of(1, 20) : pageRequest;
    }

    private WorkflowInstance requireRunningInstance(String instanceId) {
        WorkflowInstance instance = requireInstance(instanceId);
        if (instance.getInstanceStatus() != WorkflowInstanceStatus.RUNNING) {
            throw new PlatformException("workflow instance is not running: " + instance.getId());
        }
        return instance;
    }

    private WorkflowInstance requireInstance(String instanceId) {
        String validInstanceId = requireText(instanceId, "workflow instance id must not be blank");
        WorkflowInstance instance = instanceDao.findById(validInstanceId);
        if (instance == null) {
            throw new PlatformException("workflow instance not found: " + validInstanceId);
        }
        return instance;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(message);
        }
        return value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
