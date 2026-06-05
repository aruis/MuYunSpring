package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.id.Ids;
import net.ximatai.muyun.spring.common.model.EntityLifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class WorkflowTaskActionService {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final WorkflowTaskDao taskDao;
    private final WorkflowInstanceDao instanceDao;
    private final WorkflowNodeInstanceDao nodeInstanceDao;
    private final WorkflowRouteInstanceDao routeInstanceDao;
    private final WorkflowEventDao eventDao;
    private final WorkflowRuntimeEventFactory eventFactory;
    private final WorkflowApprovalTaskPolicyService approvalTaskPolicyService;
    private final WorkflowActionPolicyService actionPolicyService;
    private final WorkflowRuntimeProgressionService progressionService;
    private final Optional<WorkflowApprovalSummaryWriter> approvalSummaryWriter;

    public WorkflowTaskActionService(WorkflowTaskDao taskDao,
                                     WorkflowInstanceDao instanceDao,
                                     WorkflowNodeInstanceDao nodeInstanceDao,
                                     WorkflowEventDao eventDao,
                                     WorkflowRuntimeEventFactory eventFactory,
                                     WorkflowApprovalTaskPolicyService approvalTaskPolicyService,
                                     WorkflowRuntimeProgressionService progressionService) {
        this(taskDao, instanceDao, nodeInstanceDao, null, eventDao, eventFactory,
                approvalTaskPolicyService, new WorkflowActionPolicyService(), progressionService, Optional.empty());
    }

    @Autowired
    public WorkflowTaskActionService(WorkflowTaskDao taskDao,
                                     WorkflowInstanceDao instanceDao,
                                     WorkflowNodeInstanceDao nodeInstanceDao,
                                     WorkflowRouteInstanceDao routeInstanceDao,
                                     WorkflowEventDao eventDao,
                                     WorkflowRuntimeEventFactory eventFactory,
                                     WorkflowApprovalTaskPolicyService approvalTaskPolicyService,
                                     WorkflowActionPolicyService actionPolicyService,
                                     WorkflowRuntimeProgressionService progressionService,
                                     Optional<WorkflowApprovalSummaryWriter> approvalSummaryWriter) {
        this.taskDao = taskDao;
        this.instanceDao = instanceDao;
        this.nodeInstanceDao = nodeInstanceDao;
        this.routeInstanceDao = routeInstanceDao;
        this.eventDao = eventDao;
        this.eventFactory = eventFactory;
        this.approvalTaskPolicyService = approvalTaskPolicyService;
        this.actionPolicyService = actionPolicyService == null ? new WorkflowActionPolicyService() : actionPolicyService;
        this.progressionService = progressionService;
        this.approvalSummaryWriter = approvalSummaryWriter == null ? Optional.empty() : approvalSummaryWriter;
    }

    @Transactional
    public WorkflowTaskActionResult approve(WorkflowTaskActionRequest request) {
        WorkflowTask task = requireTodoTask(request);
        if (task.getTaskKind() != WorkflowTaskKind.APPROVAL) {
            throw new PlatformException("workflow task is not an approval task: " + request.taskId());
        }
        WorkflowInstance instance = requireInstance(task);
        WorkflowNodeInstance node = requireNode(task);
        Instant now = operatedAt(request);
        String operatorId = operatorId(request);
        actionPolicyService.requireTaskOperator(task, "approve", operatorId);
        task.setTaskStatus(WorkflowTaskStatus.DONE);
        task.setActualProcessorId(operatorId);
        task.setDecision("approve");
        task.setResultMessage(request.reason());
        task.setCompletedAt(now);
        updateTask(task, now);

        List<WorkflowTask> nodeTasks = nodeTasks(task);
        List<WorkflowTask> effectiveTasks = replaceTask(nodeTasks, task);
        if (approvalTaskPolicyService.isNodePassed(node.getApprovalMode(), node.getApprovalRatio(), effectiveTasks)) {
            node.setNodeStatus(WorkflowNodeStatus.COMPLETED);
            node.setApprovedTaskCount(countStatus(effectiveTasks, WorkflowTaskStatus.DONE));
            node.setCompletedTaskCount(countCompleted(effectiveTasks));
            node.setCompletedAt(now);
            updateNode(node, now);
            if (approvalTaskPolicyService.shouldSkipPendingSiblings(node.getApprovalMode(), node.getApprovalRatio(),
                    effectiveTasks)) {
                skipPendingSiblings(instance, task, effectiveTasks, operatorId, now);
            }
        }
        WorkflowEvent event = eventFactory.taskCompleted(instance, task, "approve", operatorId, request.reason(), now);
        eventDao.insert(event);
        if (node.getNodeStatus() == WorkflowNodeStatus.COMPLETED) {
            progressionService.advanceFromNode(instance.getId(), node.getNodeKey(), operatorId, now);
        }
        return WorkflowTaskActionResult.of(task, node, instance, event);
    }

    @Transactional
    public WorkflowTaskActionResult reject(WorkflowTaskActionRequest request) {
        WorkflowTask task = requireTodoTask(request);
        if (task.getTaskKind() != WorkflowTaskKind.APPROVAL) {
            throw new PlatformException("workflow task is not an approval task: " + request.taskId());
        }
        WorkflowInstance instance = requireInstance(task);
        WorkflowNodeInstance node = requireNode(task);
        Instant now = operatedAt(request);
        String operatorId = operatorId(request);
        actionPolicyService.requireTaskOperator(task, "reject", operatorId, request.reason());
        WorkflowRejectResubmitMode resubmitMode = request.rejectResubmitMode() == null
                ? WorkflowRejectResubmitMode.RESTART
                : request.rejectResubmitMode();
        task.setTaskStatus(WorkflowTaskStatus.REJECTED);
        task.setActualProcessorId(operatorId);
        task.setDecision("reject");
        task.setResultMessage(request.reason());
        task.setCompletedAt(now);
        updateTask(task, now);
        cancelOtherTodoTasks(instance, task.getId(), WorkflowTaskStatus.CANCELED, "reject", operatorId,
                request.reason(), now);

        node.setNodeStatus(WorkflowNodeStatus.REJECTED);
        node.setRejectedTaskCount(value(node.getRejectedTaskCount()) + 1);
        node.setCompletedAt(now);
        updateNode(node, now);

        instance.setInstanceStatus(WorkflowInstanceStatus.REJECTED);
        if (Boolean.TRUE.equals(instance.getApprovalEnabled())) {
            instance.setApprovalStatus(WorkflowApprovalStatus.REJECTED);
        }
        instance.setRejectResubmitMode(resubmitMode);
        instance.setRejectReturnNodeKey(resubmitMode == WorkflowRejectResubmitMode.RETURN_TO_ME
                ? node.getNodeKey() : null);
        instance.setRejectReturnOwnerId(resubmitMode == WorkflowRejectResubmitMode.RETURN_TO_ME
                ? operatorId : null);
        instance.setCurrentNodeKeys(null);
        instance.setLastActionCode("reject");
        instance.setLastActionReason(request.reason());
        instance.setLastOperatorId(operatorId);
        instance.setLastOperatedAt(now);
        updateInstance(instance, now);

        WorkflowTask resubmitTask = resubmitTask(instance, task, now);
        EntityLifecycle.prepareInsert(resubmitTask, now);
        taskDao.insert(resubmitTask);
        WorkflowEvent event = eventFactory.taskRejected(instance, task, operatorId, request.reason(), now);
        eventDao.insert(event);
        eventDao.insert(eventFactory.taskCreated(instance, resubmitTask, operatorId, now));
        writeApprovalSummary(instance);
        return new WorkflowTaskActionResult(task, resubmitTask, node, instance, event);
    }

    @Transactional
    public WorkflowTaskActionResult rollback(WorkflowTaskActionRequest request) {
        WorkflowTask task = requireTodoTask(request);
        if (task.getTaskKind() != WorkflowTaskKind.APPROVAL) {
            throw new PlatformException("workflow task is not an approval task: " + request.taskId());
        }
        WorkflowInstance instance = requireRunningInstance(task);
        WorkflowNodeInstance currentNode = requireNode(task);
        if (currentNode.getNodeType() != WorkflowNodeType.APPROVAL) {
            throw new PlatformException("workflow rollback only supports approval node task: " + request.taskId());
        }
        Instant now = operatedAt(request);
        String operatorId = operatorId(request);
        actionPolicyService.requireTaskOperator(task, "rollback", operatorId, request.reason());
        RollbackTarget target = previousApprovalNode(instance.getId(), currentNode);
        rejectRollbackWithOtherActiveNodes(instance.getId(), task);

        task.setTaskStatus(WorkflowTaskStatus.ROLLED_BACK);
        task.setActualProcessorId(operatorId);
        task.setDecision("rollback");
        task.setResultMessage(request.reason());
        task.setCompletedAt(now);
        updateTask(task, now);
        cancelOtherTodoTasks(instance, task.getId(), WorkflowTaskStatus.ROLLED_BACK, "rollback", operatorId,
                request.reason(), now);

        currentNode.setNodeStatus(WorkflowNodeStatus.ROLLED_BACK);
        currentNode.setRollbackTargetNodeKey(target.node().getNodeKey());
        currentNode.setCompletedAt(now);
        updateNode(currentNode, now);

        WorkflowNodeInstance previousNode = target.node();
        resetApprovalNodeForRetry(previousNode, now);
        updateNode(previousNode, now);
        for (WorkflowNodeInstance intermediateNode : target.intermediateNodes()) {
            intermediateNode.setNodeStatus(WorkflowNodeStatus.WAITING);
            intermediateNode.setCompletedAt(null);
            updateNode(intermediateNode, now);
        }

        for (WorkflowRouteInstance route : target.routes()) {
            resetRouteForRetry(route);
            updateRoute(route, now);
        }

        instance.setInstanceStatus(WorkflowInstanceStatus.RUNNING);
        if (Boolean.TRUE.equals(instance.getApprovalEnabled())) {
            instance.setApprovalStatus(WorkflowApprovalStatus.PROCESSING);
        }
        instance.setCurrentNodeKeys(previousNode.getNodeKey());
        instance.setLastActionCode("rollback");
        instance.setLastActionReason(request.reason());
        instance.setLastOperatorId(operatorId);
        instance.setLastOperatedAt(now);
        updateInstance(instance, now);

        WorkflowTask createdTask = approvalTaskForNode(instance, previousNode, previousAssignee(previousNode), now);
        EntityLifecycle.prepareInsert(createdTask, now);
        taskDao.insert(createdTask);
        WorkflowEvent event = eventFactory.nodeRolledBack(instance, currentNode, operatorId, request.reason(), now);
        eventDao.insert(event);
        eventDao.insert(eventFactory.taskCreated(instance, previousNode, createdTask, operatorId, now));
        writeApprovalSummary(instance);
        return new WorkflowTaskActionResult(task, createdTask, previousNode, instance, event);
    }

    @Transactional
    public WorkflowTaskActionResult resubmit(WorkflowTaskActionRequest request) {
        WorkflowTask task = requireTodoTask(request);
        if (task.getTaskKind() != WorkflowTaskKind.RESUBMIT) {
            throw new PlatformException("workflow task is not a resubmit task: " + request.taskId());
        }
        WorkflowInstance instance = requireInstance(task);
        if (instance.getInstanceStatus() != WorkflowInstanceStatus.REJECTED) {
            throw new PlatformException("workflow instance is not rejected: " + instance.getId());
        }
        WorkflowRejectResubmitMode mode = instance.getRejectResubmitMode() == null
                ? WorkflowRejectResubmitMode.RESTART
                : instance.getRejectResubmitMode();
        if (mode == WorkflowRejectResubmitMode.RESTART) {
            throw new PlatformException("workflow restart resubmit requires new workflow submit entry: "
                    + instance.getId());
        }
        Instant now = operatedAt(request);
        String operatorId = operatorId(request);
        actionPolicyService.requireTaskOperator(task, "resubmit", operatorId);
        task.setTaskStatus(WorkflowTaskStatus.DONE);
        task.setActualProcessorId(operatorId);
        task.setDecision("resubmit");
        task.setResultMessage(request.reason());
        task.setCompletedAt(now);
        updateTask(task, now);
        WorkflowNodeInstance node = rejectReturnNode(instance);
        String assigneeId = requireText(instance.getRejectReturnOwnerId(),
                "workflow reject return owner id must not be blank");
        resetApprovalNodeForRetry(node, now);
        updateNode(node, now);
        instance.setInstanceStatus(WorkflowInstanceStatus.RUNNING);
        if (Boolean.TRUE.equals(instance.getApprovalEnabled())) {
            instance.setApprovalStatus(WorkflowApprovalStatus.PROCESSING);
        }
        instance.setCurrentNodeKeys(node.getNodeKey());
        WorkflowTask createdTask = approvalTaskForNode(instance, node, assigneeId, now);
        EntityLifecycle.prepareInsert(createdTask, now);
        taskDao.insert(createdTask);
        eventDao.insert(eventFactory.taskCreated(instance, node, createdTask, operatorId, now));
        instance.setLastActionCode("resubmit_return_to_me");
        instance.setLastActionReason(request.reason());
        instance.setLastOperatorId(operatorId);
        instance.setLastOperatedAt(now);
        updateInstance(instance, now);
        WorkflowEvent event = eventFactory.taskResubmitted(instance, task, operatorId, request.reason(), now);
        eventDao.insert(event);
        writeApprovalSummary(instance);
        return new WorkflowTaskActionResult(task, createdTask, node, instance, event);
    }

    @Transactional
    public WorkflowTaskActionResult completeBusinessTask(WorkflowTaskActionRequest request) {
        WorkflowTask task = requireTodoTask(request);
        if (task.getTaskKind() != WorkflowTaskKind.BUSINESS) {
            throw new PlatformException("workflow task is not a business task: " + request.taskId());
        }
        WorkflowInstance instance = requireInstance(task);
        WorkflowNodeInstance node = requireNode(task);
        Instant now = operatedAt(request);
        String operatorId = operatorId(request);
        actionPolicyService.requireTaskOperator(task, "complete", operatorId);
        task.setTaskStatus(WorkflowTaskStatus.DONE);
        task.setActualProcessorId(operatorId);
        task.setDecision("complete");
        task.setResultMessage(request.reason());
        task.setCompletedAt(now);
        updateTask(task, now);
        node.setNodeStatus(WorkflowNodeStatus.COMPLETED);
        node.setCompletedTaskCount(value(node.getCompletedTaskCount()) + 1);
        node.setCompletedAt(now);
        updateNode(node, now);
        WorkflowEvent event = eventFactory.taskCompleted(instance, task, "complete", operatorId,
                request.reason(), now);
        eventDao.insert(event);
        progressionService.advanceFromNode(instance.getId(), node.getNodeKey(), operatorId, now);
        return WorkflowTaskActionResult.of(task, node, instance, event);
    }

    @Transactional
    public WorkflowTaskActionResult notice(WorkflowTaskActionRequest request) {
        WorkflowTask task = requireTodoTask(request);
        if (task.getTaskKind() != WorkflowTaskKind.NOTICE) {
            throw new PlatformException("workflow task is not a notice task: " + request.taskId());
        }
        WorkflowInstance instance = requireInstance(task);
        Instant now = operatedAt(request);
        String operatorId = operatorId(request);
        actionPolicyService.requireTaskOperator(task, "notice", operatorId);
        task.setTaskStatus(WorkflowTaskStatus.NOTICED);
        task.setActualProcessorId(operatorId);
        task.setDecision("notice");
        task.setResultMessage(request.reason());
        task.setCompletedAt(now);
        updateTask(task, now);
        WorkflowEvent event = eventFactory.taskCompleted(instance, task, "notice", operatorId,
                request.reason(), now);
        eventDao.insert(event);
        return WorkflowTaskActionResult.of(task, event);
    }

    @Transactional
    public WorkflowTaskActionResult transfer(WorkflowTaskActionRequest request) {
        WorkflowTask task = requireTodoTask(request);
        String targetAssigneeId = requireText(request.targetAssigneeId(), "workflow target assignee id must not be blank");
        WorkflowInstance instance = requireInstance(task);
        Instant now = operatedAt(request);
        String operatorId = operatorId(request);
        actionPolicyService.requireTaskOperator(task, "transfer", operatorId, request.reason());
        task.setTaskStatus(WorkflowTaskStatus.TRANSFERRED);
        task.setTransferredBy(operatorId);
        task.setTransferredAt(now);
        task.setResultMessage(request.reason());
        task.setCompletedAt(now);
        updateTask(task, now);

        WorkflowTask createdTask = transferredTask(task, targetAssigneeId, operatorId, now);
        EntityLifecycle.prepareInsert(createdTask, now);
        taskDao.insert(createdTask);

        WorkflowEvent event = eventFactory.taskTransferred(instance, task, operatorId,
                "transfer to " + targetAssigneeId, request.reason(), now);
        eventDao.insert(event);
        return WorkflowTaskActionResult.transferred(task, createdTask, event);
    }

    @Transactional
    public WorkflowTaskActionResult invalidate(WorkflowTaskActionRequest request) {
        WorkflowTask task = requireTodoTask(request);
        WorkflowInstance instance = requireInstance(task);
        Instant now = operatedAt(request);
        String operatorId = operatorId(request);
        actionPolicyService.requireTaskOperator(task, "invalidate", operatorId, request.reason());
        task.setTaskStatus(WorkflowTaskStatus.INVALIDATED);
        task.setActualProcessorId(operatorId);
        task.setDecision("invalidate");
        task.setResultMessage(request.reason());
        task.setCompletedAt(now);
        updateTask(task, now);
        WorkflowEvent event = eventFactory.taskInvalidated(instance, task, operatorId, request.reason(), now);
        eventDao.insert(event);
        return WorkflowTaskActionResult.of(task, event);
    }

    @Transactional
    public WorkflowTaskActionResult cancel(WorkflowTaskActionRequest request) {
        WorkflowTask task = requireTodoTask(request);
        WorkflowInstance instance = requireInstance(task);
        Instant now = operatedAt(request);
        String operatorId = operatorId(request);
        actionPolicyService.requireTaskOperator(task, "cancel", operatorId, request.reason());
        task.setTaskStatus(WorkflowTaskStatus.CANCELED);
        task.setActualProcessorId(operatorId);
        task.setDecision("cancel");
        task.setResultMessage(request.reason());
        task.setCompletedAt(now);
        updateTask(task, now);
        WorkflowEvent event = eventFactory.taskCanceled(instance, task, operatorId, request.reason(), now);
        eventDao.insert(event);
        return WorkflowTaskActionResult.of(task, event);
    }

    private WorkflowTask transferredTask(WorkflowTask source, String targetAssigneeId,
                                         String operatorId, Instant now) {
        WorkflowTask task = new WorkflowTask();
        task.setId(Ids.newId());
        task.setTenantId(source.getTenantId());
        task.setInstanceId(source.getInstanceId());
        task.setNodeInstanceId(source.getNodeInstanceId());
        task.setTaskKind(source.getTaskKind());
        task.setTaskStatus(WorkflowTaskStatus.TODO);
        task.setParentTaskId(source.getParentTaskId());
        task.setOriginTaskId(source.getOriginTaskId() == null ? source.getId() : source.getOriginTaskId());
        task.setAssignmentKind(WorkflowAssignmentKind.TRANSFERRED);
        task.setOwnerId(source.getOwnerId());
        task.setOriginalAssigneeId(source.getOriginalAssigneeId() == null
                ? source.getAssigneeId()
                : source.getOriginalAssigneeId());
        task.setAssigneeId(targetAssigneeId);
        task.setTransferredFromUserId(source.getAssigneeId());
        task.setTransferredBy(operatorId);
        task.setTransferredAt(now);
        task.setCheckStatus(source.getCheckStatus());
        task.setAssignmentPolicyText(source.getAssignmentPolicyText());
        task.setAssignmentSnapshotText(source.getAssignmentSnapshotText());
        task.setDueAt(source.getDueAt());
        return task;
    }

    private WorkflowTask resubmitTask(WorkflowInstance instance, WorkflowTask rejectedTask, Instant now) {
        WorkflowTask task = new WorkflowTask();
        task.setId(Ids.newId());
        task.setTenantId(instance.getTenantId());
        task.setInstanceId(instance.getId());
        task.setTaskKind(WorkflowTaskKind.RESUBMIT);
        task.setTaskStatus(WorkflowTaskStatus.TODO);
        task.setParentTaskId(rejectedTask.getId());
        task.setOriginTaskId(rejectedTask.getOriginTaskId() == null ? rejectedTask.getId() : rejectedTask.getOriginTaskId());
        task.setAssignmentKind(WorkflowAssignmentKind.NORMAL);
        task.setOwnerId(instance.getStartedBy());
        task.setOriginalAssigneeId(instance.getStartedBy());
        task.setAssigneeId(requireText(instance.getStartedBy(), "workflow started by must not be blank"));
        task.setCheckStatus(WorkflowTaskCheckStatus.NO_CHECK);
        return task;
    }

    private WorkflowTask approvalTaskForNode(WorkflowInstance instance, WorkflowNodeInstance node,
                                             String assigneeId, Instant now) {
        WorkflowTask task = new WorkflowTask();
        task.setId(Ids.newId());
        task.setTenantId(instance.getTenantId());
        task.setInstanceId(instance.getId());
        task.setNodeInstanceId(node.getId());
        task.setTaskKind(WorkflowTaskKind.APPROVAL);
        task.setTaskStatus(WorkflowTaskStatus.TODO);
        task.setAssignmentKind(WorkflowAssignmentKind.NORMAL);
        task.setOwnerId(assigneeId);
        task.setOriginalAssigneeId(assigneeId);
        task.setAssigneeId(requireText(assigneeId, "workflow approval assignee id must not be blank"));
        task.setCheckStatus(WorkflowTaskCheckStatus.NO_CHECK);
        return task;
    }

    private WorkflowTask requireTodoTask(WorkflowTaskActionRequest request) {
        String taskId = requireText(request == null ? null : request.taskId(), "workflow task id must not be blank");
        WorkflowTask task = taskDao.findById(taskId);
        if (task == null) {
            throw new PlatformException("workflow task not found: " + taskId);
        }
        if (task.getTaskStatus() != WorkflowTaskStatus.TODO) {
            throw new PlatformException("workflow task is not todo: " + taskId);
        }
        return task;
    }

    private WorkflowInstance requireInstance(WorkflowTask task) {
        WorkflowInstance instance = instanceDao.findById(task.getInstanceId());
        if (instance == null) {
            throw new PlatformException("workflow instance not found: " + task.getInstanceId());
        }
        return instance;
    }

    private WorkflowInstance requireRunningInstance(WorkflowTask task) {
        WorkflowInstance instance = requireInstance(task);
        if (instance.getInstanceStatus() != WorkflowInstanceStatus.RUNNING) {
            throw new PlatformException("workflow instance is not running: " + instance.getId());
        }
        return instance;
    }

    private WorkflowNodeInstance requireNode(WorkflowTask task) {
        WorkflowNodeInstance node = nodeInstanceDao.findById(task.getNodeInstanceId());
        if (node == null) {
            throw new PlatformException("workflow node instance not found: " + task.getNodeInstanceId());
        }
        return node;
    }

    private List<WorkflowTask> nodeTasks(WorkflowTask task) {
        return taskDao.query(Criteria.of()
                .eq("instanceId", task.getInstanceId())
                .eq("nodeInstanceId", task.getNodeInstanceId()), ALL);
    }

    private List<WorkflowTask> instanceTodoTasks(String instanceId) {
        return taskDao.query(Criteria.of()
                .eq("instanceId", instanceId)
                .eq("taskStatus", WorkflowTaskStatus.TODO), ALL);
    }

    private void cancelOtherTodoTasks(WorkflowInstance instance, String currentTaskId, WorkflowTaskStatus taskStatus,
                                      String decision, String operatorId, String message, Instant now) {
        for (WorkflowTask other : instanceTodoTasks(instance.getId())) {
            if (currentTaskId.equals(other.getId())) {
                continue;
            }
            other.setTaskStatus(taskStatus);
            other.setActualProcessorId(operatorId);
            other.setDecision(decision);
            other.setResultMessage(message);
            other.setCompletedAt(now);
            updateTask(other, now);
            eventDao.insert(taskStatus == WorkflowTaskStatus.CANCELED
                    ? eventFactory.taskCanceled(instance, other, operatorId, message, now)
                    : eventFactory.nodeRolledBack(instance, requireNode(other), operatorId, message, now));
        }
    }

    private void rejectRollbackWithOtherActiveNodes(String instanceId, WorkflowTask currentTask) {
        for (WorkflowTask other : instanceTodoTasks(instanceId)) {
            if (currentTask.getId().equals(other.getId())) {
                continue;
            }
            if (!sameText(currentTask.getNodeInstanceId(), other.getNodeInstanceId())) {
                throw new PlatformException("workflow rollback only supports single active node in first version: "
                        + instanceId);
            }
        }
    }

    private RollbackTarget previousApprovalNode(String instanceId, WorkflowNodeInstance currentNode) {
        WorkflowNodeInstance cursor = currentNode;
        List<WorkflowRouteInstance> pathRoutes = new ArrayList<>();
        List<WorkflowNodeInstance> intermediateNodes = new ArrayList<>();
        for (int depth = 0; depth < 64; depth++) {
            List<WorkflowRouteInstance> incoming = requireRouteDao().query(Criteria.of()
                    .eq("instanceId", instanceId)
                    .eq("targetNodeKey", cursor.getNodeKey())
                    .eq("routeStatus", WorkflowRouteStatus.EFFECTIVE), ALL);
            if (incoming.isEmpty()) {
                throw new PlatformException("workflow rollback previous approval node not found: "
                        + currentNode.getNodeKey());
            }
            if (incoming.size() != 1) {
                throw new PlatformException("workflow rollback only supports linear effective route: "
                        + currentNode.getNodeKey());
            }
            WorkflowRouteInstance route = incoming.get(0);
            rejectComplexRollbackRoute(route);
            pathRoutes.add(route);
            WorkflowNodeInstance source = singleNode(instanceId, route.getSourceNodeKey());
            if (source.getNodeType() == WorkflowNodeType.APPROVAL) {
                if (source.getNodeStatus() != WorkflowNodeStatus.COMPLETED) {
                    throw new PlatformException("workflow rollback previous approval node is not completed: "
                            + source.getNodeKey());
                }
                return new RollbackTarget(source, pathRoutes, intermediateNodes);
            }
            if (source.getNodeType() == WorkflowNodeType.BRANCH || source.getNodeType() == WorkflowNodeType.CONVERGE) {
                throw new PlatformException("workflow rollback does not support branch or converge path: "
                        + source.getNodeKey());
            }
            intermediateNodes.add(source);
            cursor = source;
        }
        throw new PlatformException("workflow rollback path exceeded max depth: " + currentNode.getNodeKey());
    }

    private WorkflowNodeInstance singleNode(String instanceId, String nodeKey) {
        List<WorkflowNodeInstance> sources = nodeInstanceDao.query(Criteria.of()
                .eq("instanceId", instanceId)
                .eq("nodeKey", nodeKey), ALL);
        if (sources.size() != 1) {
            throw new PlatformException("workflow rollback source node is not unique: " + nodeKey);
        }
        return sources.get(0);
    }

    private void rejectComplexRollbackRoute(WorkflowRouteInstance route) {
        if (hasText(route.getBranchNodeKey()) || hasText(route.getBranchRunId())
                || hasText(route.getConvergeNodeKey()) || hasText(route.getConvergeRunId())
                || hasText(route.getParentRouteId())) {
            throw new PlatformException("workflow rollback only supports linear route in first version: "
                    + route.getRouteKey());
        }
    }

    private void resetApprovalNodeForRetry(WorkflowNodeInstance node, Instant now) {
        node.setNodeStatus(WorkflowNodeStatus.ACTIVE);
        node.setCompletedAt(null);
        node.setRollbackTargetNodeKey(null);
        node.setCompletedTaskCount(0);
        node.setApprovedTaskCount(0);
        node.setRejectedTaskCount(0);
        node.setActivatedAt(now);
    }

    private void resetRouteForRetry(WorkflowRouteInstance route) {
        route.setRouteStatus(WorkflowRouteStatus.CANDIDATE);
        route.setRouteReason(null);
        route.setConditionMatched(false);
        route.setSelectedBy(null);
        route.setSelectedAt(null);
        route.setArrivedAt(null);
        route.setClosedByRouteId(null);
        route.setClosedReason(null);
        route.setInvalidatedByActionId(null);
        route.setInvalidatedAt(null);
    }

    private String previousAssignee(WorkflowNodeInstance previousNode) {
        return taskDao.query(Criteria.of()
                        .eq("instanceId", previousNode.getInstanceId())
                        .eq("nodeInstanceId", previousNode.getId()), ALL)
                .stream()
                .filter(task -> task.getTaskKind() == WorkflowTaskKind.APPROVAL)
                .filter(task -> task.getTaskStatus() == WorkflowTaskStatus.DONE)
                .sorted(Comparator.comparing(WorkflowTask::getCompletedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(task -> firstText(task.getActualProcessorId(), task.getAssigneeId(), task.getOwnerId()))
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElseThrow(() -> new PlatformException("workflow rollback previous approval assignee not found: "
                        + previousNode.getNodeKey()));
    }

    private WorkflowNodeInstance rejectReturnNode(WorkflowInstance instance) {
        String nodeKey = requireText(instance.getRejectReturnNodeKey(),
                "workflow reject return node key must not be blank");
        List<WorkflowNodeInstance> nodes = nodeInstanceDao.query(Criteria.of()
                .eq("instanceId", instance.getId())
                .eq("nodeKey", nodeKey), ALL);
        if (nodes.size() != 1) {
            throw new PlatformException("workflow reject return node is not unique: " + nodeKey);
        }
        return nodes.get(0);
    }

    private List<WorkflowTask> replaceTask(List<WorkflowTask> tasks, WorkflowTask changed) {
        List<WorkflowTask> result = new ArrayList<>();
        boolean replaced = false;
        for (WorkflowTask task : tasks == null ? List.<WorkflowTask>of() : tasks) {
            if (changed.getId().equals(task.getId())) {
                result.add(changed);
                replaced = true;
            } else {
                result.add(task);
            }
        }
        if (!replaced) {
            result.add(changed);
        }
        return result;
    }

    private void skipPendingSiblings(WorkflowInstance instance, WorkflowTask completedTask,
                                     List<WorkflowTask> tasks, String operatorId, Instant now) {
        for (WorkflowTask task : tasks) {
            if (task == completedTask || completedTask.getId().equals(task.getId())
                    || task.getTaskStatus() != WorkflowTaskStatus.TODO) {
                continue;
            }
            task.setTaskStatus(WorkflowTaskStatus.SKIPPED);
            task.setDecision("skip");
            task.setResultMessage("approval node passed");
            task.setCompletedAt(now);
            updateTask(task, now);
            eventDao.insert(eventFactory.taskSkipped(instance, task, operatorId, "approval node passed", now));
        }
    }

    private void updateTask(WorkflowTask task, Instant now) {
        Integer expectedVersion = task.getVersion();
        EntityLifecycle.prepareUpdate(task, now, EntityLifecycle.nextVersion(expectedVersion));
        int updated = taskDao.updateByIdAndVersion(task, expectedVersion);
        if (updated <= 0) {
            throw new OptimisticLockException("workflow task version conflict: " + task.getId());
        }
    }

    private void updateNode(WorkflowNodeInstance node, Instant now) {
        Integer expectedVersion = node.getVersion();
        EntityLifecycle.prepareUpdate(node, now, EntityLifecycle.nextVersion(expectedVersion));
        int updated = nodeInstanceDao.updateByIdAndVersion(node, expectedVersion);
        if (updated <= 0) {
            throw new OptimisticLockException("workflow node version conflict: " + node.getId());
        }
    }

    private void updateInstance(WorkflowInstance instance, Instant now) {
        Integer expectedVersion = instance.getVersion();
        EntityLifecycle.prepareUpdate(instance, now, EntityLifecycle.nextVersion(expectedVersion));
        int updated = instanceDao.updateByIdAndVersion(instance, expectedVersion);
        if (updated <= 0) {
            throw new OptimisticLockException("workflow instance version conflict: " + instance.getId());
        }
    }

    private void updateRoute(WorkflowRouteInstance route, Instant now) {
        Integer expectedVersion = route.getVersion();
        EntityLifecycle.prepareUpdate(route, now, EntityLifecycle.nextVersion(expectedVersion));
        int updated = routeInstanceDao.updateByIdAndVersion(route, expectedVersion);
        if (updated <= 0) {
            throw new OptimisticLockException("workflow route version conflict: " + route.getId());
        }
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

    private int countStatus(List<WorkflowTask> tasks, WorkflowTaskStatus status) {
        return (int) tasks.stream().filter(task -> task.getTaskStatus() == status).count();
    }

    private int countCompleted(List<WorkflowTask> tasks) {
        return (int) tasks.stream()
                .filter(task -> task.getCompletedAt() != null || task.getTaskStatus() == WorkflowTaskStatus.DONE)
                .count();
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String operatorId(WorkflowTaskActionRequest request) {
        if (request.operatorId() != null && !request.operatorId().isBlank()) {
            return request.operatorId();
        }
        return CurrentUserContext.currentUser()
                .map(user -> user.userId())
                .filter(userId -> !userId.isBlank())
                .orElse("system");
    }

    private Instant operatedAt(WorkflowTaskActionRequest request) {
        return request.operatedAt() == null ? Instant.now() : request.operatedAt();
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(message);
        }
        return value;
    }

    private WorkflowRouteInstanceDao requireRouteDao() {
        if (routeInstanceDao == null) {
            throw new PlatformException("workflow route dao is required for rollback");
        }
        return routeInstanceDao;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean sameText(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private record RollbackTarget(WorkflowNodeInstance node, List<WorkflowRouteInstance> routes,
                                  List<WorkflowNodeInstance> intermediateNodes) {
    }
}
