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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private final WorkflowDelegationService delegationService;
    private final WorkflowDelegationCompletionNoticeService delegationCompletionNoticeService;
    private final WorkflowRuntimePluginDispatcher pluginDispatcher;

    public WorkflowTaskActionService(WorkflowTaskDao taskDao,
                                     WorkflowInstanceDao instanceDao,
                                     WorkflowNodeInstanceDao nodeInstanceDao,
                                     WorkflowEventDao eventDao,
                                     WorkflowRuntimeEventFactory eventFactory,
                                     WorkflowApprovalTaskPolicyService approvalTaskPolicyService,
                                     WorkflowRuntimeProgressionService progressionService) {
        this(taskDao, instanceDao, nodeInstanceDao, null, eventDao, eventFactory,
                approvalTaskPolicyService, new WorkflowActionPolicyService(), progressionService, Optional.empty(), null,
                null, null);
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
                                     Optional<WorkflowApprovalSummaryWriter> approvalSummaryWriter,
                                     WorkflowDelegationService delegationService,
                                     WorkflowDelegationCompletionNoticeService delegationCompletionNoticeService,
                                     WorkflowRuntimePluginDispatcher pluginDispatcher) {
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
        this.delegationService = delegationService;
        this.delegationCompletionNoticeService = delegationCompletionNoticeService;
        this.pluginDispatcher = pluginDispatcher == null ? new WorkflowRuntimePluginDispatcher(List.of()) : pluginDispatcher;
    }

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
        this(taskDao, instanceDao, nodeInstanceDao, routeInstanceDao, eventDao, eventFactory,
                approvalTaskPolicyService, actionPolicyService, progressionService, approvalSummaryWriter, null, null,
                null);
    }

    public WorkflowTaskActionService(WorkflowTaskDao taskDao,
                                     WorkflowInstanceDao instanceDao,
                                     WorkflowNodeInstanceDao nodeInstanceDao,
                                     WorkflowRouteInstanceDao routeInstanceDao,
                                     WorkflowEventDao eventDao,
                                     WorkflowRuntimeEventFactory eventFactory,
                                     WorkflowApprovalTaskPolicyService approvalTaskPolicyService,
                                     WorkflowActionPolicyService actionPolicyService,
                                     WorkflowRuntimeProgressionService progressionService,
                                     Optional<WorkflowApprovalSummaryWriter> approvalSummaryWriter,
                                     WorkflowDelegationService delegationService) {
        this(taskDao, instanceDao, nodeInstanceDao, routeInstanceDao, eventDao, eventFactory,
                approvalTaskPolicyService, actionPolicyService, progressionService, approvalSummaryWriter,
                delegationService, null, null);
    }

    public WorkflowTaskActionService(WorkflowTaskDao taskDao,
                                     WorkflowInstanceDao instanceDao,
                                     WorkflowNodeInstanceDao nodeInstanceDao,
                                     WorkflowRouteInstanceDao routeInstanceDao,
                                     WorkflowEventDao eventDao,
                                     WorkflowRuntimeEventFactory eventFactory,
                                     WorkflowApprovalTaskPolicyService approvalTaskPolicyService,
                                     WorkflowActionPolicyService actionPolicyService,
                                     WorkflowRuntimeProgressionService progressionService,
                                     Optional<WorkflowApprovalSummaryWriter> approvalSummaryWriter,
                                     WorkflowDelegationService delegationService,
                                     WorkflowRuntimePluginDispatcher pluginDispatcher) {
        this(taskDao, instanceDao, nodeInstanceDao, routeInstanceDao, eventDao, eventFactory,
                approvalTaskPolicyService, actionPolicyService, progressionService, approvalSummaryWriter,
                delegationService, null, pluginDispatcher);
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
        actionPolicyService.requireRuntimeAction(instance, "approve");
        actionPolicyService.requireTaskOperator(task, "approve", operatorId);
        dispatchTask(instance, node, task, WorkflowRuntimePluginEventType.BEFORE_APPROVE, "approve",
                operatorId, null, null, request.reason());
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
        createDelegationCompletionNotice(instance, task, operatorId, now);
        if (node.getNodeStatus() == WorkflowNodeStatus.COMPLETED) {
            progressionService.advanceFromNode(instance.getId(), node.getNodeKey(), operatorId, now);
        }
        dispatchTask(instance, node, task, WorkflowRuntimePluginEventType.AFTER_APPROVE, "approve",
                operatorId, null, null, request.reason());
        return WorkflowTaskActionResult.of(task, node, instance, event);
    }

    @Transactional
    public WorkflowTaskActionResult forceApprove(WorkflowTaskActionRequest request) {
        WorkflowTask task = requireTodoTask(request);
        if (task.getTaskKind() != WorkflowTaskKind.APPROVAL) {
            throw new PlatformException("workflow force approve only supports approval task: " + request.taskId());
        }
        WorkflowInstance instance = requireRunningInstance(task);
        WorkflowNodeInstance node = requireNode(task);
        Instant now = operatedAt(request);
        String operatorId = operatorId(request);
        actionPolicyService.requireManagementTaskAction("forceApprove", request.reason());
        task.setTaskStatus(WorkflowTaskStatus.DONE);
        task.setActualProcessorId(operatorId);
        task.setDecision("forceApprove");
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
        WorkflowEvent event = eventFactory.taskCompleted(instance, task, "forceApprove", operatorId,
                request.reason(), now);
        eventDao.insert(event);
        createDelegationCompletionNotice(instance, task, operatorId, now);
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
        actionPolicyService.requireRuntimeAction(instance, "reject");
        actionPolicyService.requireNodeTaskAction(task, node, "reject", operatorId, request.reason());
        WorkflowRejectResubmitMode resubmitMode = request.rejectResubmitMode() == null
                ? WorkflowRejectResubmitMode.RESTART
                : request.rejectResubmitMode();
        if (resubmitMode == WorkflowRejectResubmitMode.RETURN_TO_ME) {
            actionPolicyService.requireRejectReturnToMe(node);
        }
        dispatchTask(instance, node, task, WorkflowRuntimePluginEventType.BEFORE_REJECT, "reject",
                operatorId, null, null, request.reason());
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
        createDelegationCompletionNotice(instance, task, operatorId, now);
        eventDao.insert(eventFactory.taskCreated(instance, resubmitTask, operatorId, now));
        writeApprovalSummary(instance);
        dispatchTask(instance, node, task, WorkflowRuntimePluginEventType.AFTER_REJECT, "reject",
                operatorId, null, null, request.reason());
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
        actionPolicyService.requireRuntimeAction(instance, "rollback");
        actionPolicyService.requireNodeTaskAction(task, currentNode, "rollback", operatorId, request.reason());
        RollbackTarget target = previousApprovalNode(instance.getId(), currentNode);
        rejectRollbackWithOtherActiveNodes(instance.getId(), task);
        dispatchTask(instance, currentNode, task, WorkflowRuntimePluginEventType.BEFORE_ROLLBACK, "rollback",
                operatorId, null, target.node().getNodeKey(), request.reason());

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
        createDelegationCompletionNotice(instance, task, operatorId, now);
        eventDao.insert(eventFactory.taskCreated(instance, previousNode, createdTask, operatorId, now));
        writeApprovalSummary(instance);
        dispatchTask(instance, currentNode, task, WorkflowRuntimePluginEventType.AFTER_ROLLBACK, "rollback",
                operatorId, null, target.node().getNodeKey(), request.reason());
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
        actionPolicyService.requireRuntimeAction(instance, "resubmit");
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
        actionPolicyService.requireRuntimeAction(instance, "complete");
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
        createDelegationCompletionNotice(instance, task, operatorId, now);
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
        actionPolicyService.requireRuntimeAction(instance, "notice");
        actionPolicyService.requireTaskOperator(task, "notice", operatorId);
        task.setTaskStatus(WorkflowTaskStatus.NOTICED);
        if (!isDelegationCompletionNotice(task)) {
            task.setActualProcessorId(operatorId);
            task.setCompletedAt(now);
        }
        task.setDecision("notice");
        task.setResultMessage(request.reason());
        updateTask(task, now);
        WorkflowEvent event = eventFactory.taskCompleted(instance, task, "notice", operatorId,
                request.reason(), now);
        eventDao.insert(event);
        return WorkflowTaskActionResult.of(task, event);
    }

    @Transactional
    public WorkflowTaskActionResult readNotice(WorkflowTaskActionRequest request) {
        WorkflowTask task = requireNoticeReadTask(request);
        WorkflowInstance instance = requireInstance(task);
        Instant now = operatedAt(request);
        String operatorId = operatorId(request);
        actionPolicyService.requireRuntimeAction(instance, "notice");
        requireNoticeOwner(task, operatorId);
        if (task.getTaskStatus() == WorkflowTaskStatus.NOTICED) {
            return WorkflowTaskActionResult.of(task, null);
        }
        task.setTaskStatus(WorkflowTaskStatus.NOTICED);
        if (!isDelegationCompletionNotice(task)) {
            task.setActualProcessorId(operatorId);
            task.setCompletedAt(now);
        }
        task.setDecision("notice");
        task.setResultMessage(request.reason());
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
        actionPolicyService.requireRuntimeAction(instance, "transfer");
        actionPolicyService.requireTaskOperator(task, "transfer", operatorId);
        WorkflowNodeInstance node = task.getNodeInstanceId() == null ? null : nodeInstanceDao.findById(task.getNodeInstanceId());
        dispatchTask(instance, node, task, WorkflowRuntimePluginEventType.BEFORE_TRANSFER, "transfer",
                operatorId, targetAssigneeId, null, request.reason());
        task.setTaskStatus(WorkflowTaskStatus.TRANSFERRED);
        task.setActualProcessorId(operatorId);
        task.setDecision("transfer");
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
        dispatchTask(instance, node, createdTask, WorkflowRuntimePluginEventType.AFTER_TRANSFER, "transfer",
                operatorId, targetAssigneeId, null, request.reason());
        return WorkflowTaskActionResult.transferred(task, createdTask, event);
    }

    @Transactional
    public WorkflowTaskActionResult addSign(WorkflowTaskActionRequest request) {
        WorkflowTask task = requireTodoTask(request);
        if (task.getTaskKind() != WorkflowTaskKind.APPROVAL) {
            throw new PlatformException("workflow add sign only supports approval task: " + request.taskId());
        }
        WorkflowInstance instance = requireRunningInstance(task);
        WorkflowNodeInstance node = requireNode(task);
        if (node.getNodeType() != WorkflowNodeType.APPROVAL) {
            throw new PlatformException("workflow add sign only supports approval node task: " + request.taskId());
        }
        if (node.getNodeStatus() != WorkflowNodeStatus.ACTIVE) {
            throw new PlatformException("workflow add sign only supports active approval node: " + node.getNodeKey());
        }
        Instant now = operatedAt(request);
        String operatorId = operatorId(request);
        actionPolicyService.requireRuntimeAction(instance, "addSign");
        actionPolicyService.requireNodeTaskAction(task, node, "addSign", operatorId, request.reason());
        rejectMultiTodoApprovalTasks(task);
        rejectExistingUnstartedAddSignSegment(instance.getId(), node.getNodeKey());
        List<WorkflowRouteInstance> originalRoutes = outgoingCandidateRoutes(instance.getId(), node.getNodeKey());
        if (originalRoutes.isEmpty()) {
            throw new PlatformException("workflow add sign requires candidate route after current node: "
                    + node.getNodeKey());
        }
        AddSignSegmentPlan plan = buildAddSignSegmentPlan(instance, node, originalRoutes, request.addSignSegment(),
                operatorId, now);

        for (WorkflowRouteInstance route : originalRoutes) {
            route.setRouteStatus(WorkflowRouteStatus.CANCELED);
            route.setRouteReason(WorkflowRouteReason.MANUAL_UNSELECTED);
            route.setSelectedBy(operatorId);
            route.setSelectedAt(now);
            route.setClosedReason("workflow route replaced by addSign");
            route.setInvalidatedByActionId("addSign");
            route.setInvalidatedAt(now);
            updateRoute(route, now);
        }
        for (WorkflowNodeInstance addedNode : plan.nodes()) {
            EntityLifecycle.prepareInsert(addedNode, now);
            nodeInstanceDao.insert(addedNode);
        }
        for (WorkflowRouteInstance addedRoute : plan.routes()) {
            EntityLifecycle.prepareInsert(addedRoute, now);
            routeInstanceDao.insert(addedRoute);
        }

        WorkflowEvent event = eventFactory.addSign(instance, node, task, operatorId, request.reason(),
                addSignPayload(node.getNodeKey(), plan.addedNodeKeys(), originalRoutes.stream()
                        .map(WorkflowRouteInstance::getId).toList(), WorkflowAddSignEditMode.CREATE), now);
        eventDao.insert(event);
        return WorkflowTaskActionResult.addSign(task, node, instance, event, WorkflowAddSignEditMode.CREATE,
                plan.addedNodeKeys(), originalRoutes.stream().map(WorkflowRouteInstance::getId).toList());
    }

    @Transactional
    public WorkflowTaskActionResult invalidate(WorkflowTaskActionRequest request) {
        WorkflowTask task = requireTodoTask(request);
        WorkflowInstance instance = requireInstance(task);
        Instant now = operatedAt(request);
        String operatorId = operatorId(request);
        actionPolicyService.requireRuntimeAction(instance, "invalidate");
        actionPolicyService.requireTaskOperator(task, "invalidate", operatorId);
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
        actionPolicyService.requireRuntimeAction(instance, "cancel");
        actionPolicyService.requireTaskOperator(task, "cancel", operatorId);
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
        task.setDelegatedFromUserId(source.getDelegatedFromUserId());
        task.setDelegatedToUserId(source.getDelegatedToUserId());
        task.setPrincipalCanProcess(source.getPrincipalCanProcess());
        task.setDelegationPolicyId(source.getDelegationPolicyId());
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
        applyDelegation(instance, task);
        return task;
    }

    private void applyDelegation(WorkflowInstance instance, WorkflowTask task) {
        if (delegationService == null) {
            return;
        }
        WorkflowDelegationMatch match = delegationService.match(task.getOriginalAssigneeId(),
                instance.getModuleAlias(), instance.getAuthOrgId());
        if (match == null) {
            return;
        }
        task.setAssignmentKind(WorkflowAssignmentKind.DELEGATED);
        task.setAssigneeId(match.delegateUserId());
        task.setDelegatedFromUserId(match.principalUserId());
        task.setDelegatedToUserId(match.delegateUserId());
        task.setPrincipalCanProcess(match.principalCanProcess());
        task.setDelegationPolicyId(match.delegationPolicyId());
        task.setAssignmentSnapshotText(match.snapshotText());
    }

    private void dispatchTask(WorkflowInstance instance,
                              WorkflowNodeInstance node,
                              WorkflowTask task,
                              WorkflowRuntimePluginEventType eventType,
                              String actionCode,
                              String operatorId,
                              String targetAssigneeId,
                              String rollbackTargetNodeKey,
                              String reason) {
        pluginDispatcher.dispatch(new WorkflowRuntimePluginContext(eventType, actionCode,
                instance == null ? null : instance.getModuleAlias(),
                instance == null ? null : instance.getRecordId(),
                instance == null ? null : instance.getId(),
                node == null ? null : node.getNodeKey(),
                task == null ? null : task.getId(),
                operatorId, targetAssigneeId, rollbackTargetNodeKey, null, reason, instance, node, task));
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

    private WorkflowTask requireNoticeReadTask(WorkflowTaskActionRequest request) {
        String taskId = requireText(request == null ? null : request.taskId(), "workflow task id must not be blank");
        WorkflowTask task = taskDao.findById(taskId);
        if (task == null) {
            throw new PlatformException("workflow task not found: " + taskId);
        }
        if (task.getTaskKind() != WorkflowTaskKind.NOTICE) {
            throw new PlatformException("workflow task is not a notice task: " + taskId);
        }
        if (task.getTaskStatus() != WorkflowTaskStatus.TODO && task.getTaskStatus() != WorkflowTaskStatus.NOTICED) {
            throw new PlatformException("workflow notice task is not readable: " + taskId);
        }
        return task;
    }

    private void requireNoticeOwner(WorkflowTask task, String operatorId) {
        String validOperatorId = requireText(operatorId, "workflow operator id must not be blank");
        if (!validOperatorId.equals(task.getAssigneeId())) {
            throw new PlatformException("workflow notice reader is not assignee: " + task.getId());
        }
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

    private void rejectMultiTodoApprovalTasks(WorkflowTask task) {
        long todoApprovalCount = nodeTasks(task).stream()
                .filter(nodeTask -> nodeTask.getTaskKind() == WorkflowTaskKind.APPROVAL)
                .filter(nodeTask -> nodeTask.getTaskStatus() == WorkflowTaskStatus.TODO)
                .count();
        if (todoApprovalCount > 1) {
            throw new PlatformException("workflow add sign does not support multi todo approval tasks on one node: "
                    + task.getNodeInstanceId());
        }
    }

    private void rejectExistingUnstartedAddSignSegment(String instanceId, String sourceNodeKey) {
        List<WorkflowNodeInstance> existingNodes = nodeInstanceDao.query(Criteria.of()
                .eq("instanceId", instanceId)
                .eq("addedByAddSign", true)
                .eq("addSignSourceNodeKey", sourceNodeKey), ALL);
        if (existingNodes.isEmpty()) {
            return;
        }
        for (WorkflowNodeInstance existingNode : existingNodes) {
            if (existingNode.getNodeStatus() != WorkflowNodeStatus.WAITING) {
                throw new PlatformException("workflow add sign segment is already effective: " + sourceNodeKey);
            }
        }
        Set<String> nodeIds = existingNodes.stream()
                .map(WorkflowNodeInstance::getId)
                .collect(java.util.stream.Collectors.toSet());
        boolean hasTasks = taskDao.query(Criteria.of()
                        .eq("instanceId", instanceId), ALL).stream()
                .anyMatch(existingTask -> nodeIds.contains(existingTask.getNodeInstanceId()));
        if (hasTasks) {
            throw new PlatformException("workflow add sign segment is already effective: " + sourceNodeKey);
        }
        throw new PlatformException("workflow add sign replace is not supported in first version: "
                + WorkflowAddSignEditMode.UNSUPPORTED_REPLACE_FAIL_FAST.getCode());
    }

    private List<WorkflowRouteInstance> outgoingCandidateRoutes(String instanceId, String sourceNodeKey) {
        return requireRouteDao().query(Criteria.of()
                .eq("instanceId", instanceId)
                .eq("sourceNodeKey", sourceNodeKey)
                .eq("routeStatus", WorkflowRouteStatus.CANDIDATE), ALL);
    }

    private AddSignSegmentPlan buildAddSignSegmentPlan(WorkflowInstance instance,
                                                       WorkflowNodeInstance sourceNode,
                                                       List<WorkflowRouteInstance> originalRoutes,
                                                       WorkflowAddSignSegment segment,
                                                       String operatorId,
                                                       Instant now) {
        if (segment == null) {
            throw new PlatformException("workflow add sign segment must not be null");
        }
        if (segment.nodeDefinitions().isEmpty()) {
            throw new PlatformException("workflow add sign segment must contain nodes");
        }
        if (segment.linkDefinitions().isEmpty()) {
            throw new PlatformException("workflow add sign segment must contain links");
        }
        if (originalRoutes.size() != 1) {
            throw new PlatformException("workflow add sign only supports single original next node in first version: "
                    + sourceNode.getNodeKey());
        }
        String originalNextNodeKey = requireText(originalRoutes.getFirst().getTargetNodeKey(),
                "workflow original next node key must not be blank");
        Set<String> existingNodeKeys = nodeInstanceDao.query(Criteria.of()
                        .eq("instanceId", instance.getId()), ALL).stream()
                .map(WorkflowNodeInstance::getNodeKey)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<String, WorkflowNodeDefinition> definitionsByKey = new LinkedHashMap<>();
        for (WorkflowNodeDefinition definition : segment.nodeDefinitions()) {
            String nodeKey = requireText(definition.getNodeKey(), "workflow add sign node key must not be blank");
            if (definitionsByKey.containsKey(nodeKey)) {
                throw new PlatformException("workflow add sign node key duplicated: " + nodeKey);
            }
            if (existingNodeKeys.contains(nodeKey)) {
                throw new PlatformException("workflow add sign node key conflicts with instance node: " + nodeKey);
            }
            validateAddSignNodeDefinition(definition);
            definitionsByKey.put(nodeKey, definition);
        }
        Set<String> routeKeys = new LinkedHashSet<>();
        for (WorkflowLinkDefinition definition : segment.linkDefinitions()) {
            String routeKey = requireText(definition.getRouteKey(), "workflow add sign route key must not be blank");
            if (!routeKeys.add(routeKey)) {
                throw new PlatformException("workflow add sign route key duplicated: " + routeKey);
            }
        }
        validateLinearAddSignLinks(sourceNode.getNodeKey(), originalNextNodeKey, definitionsByKey.keySet(),
                segment.linkDefinitions());
        List<WorkflowNodeInstance> nodes = segment.nodeDefinitions().stream()
                .map(definition -> addSignNode(instance, sourceNode, definition, operatorId, now))
                .toList();
        List<WorkflowRouteInstance> routes = segment.linkDefinitions().stream()
                .map(definition -> addSignRoute(instance, sourceNode, definition, operatorId, now))
                .toList();
        return new AddSignSegmentPlan(nodes, routes,
                nodes.stream().map(WorkflowNodeInstance::getNodeKey).toList());
    }

    private void validateLinearAddSignLinks(String sourceNodeKey,
                                            String originalNextNodeKey,
                                            Set<String> addedNodeKeys,
                                            List<WorkflowLinkDefinition> linkDefinitions) {
        Set<String> allowed = new LinkedHashSet<>();
        allowed.add(sourceNodeKey);
        allowed.addAll(addedNodeKeys);
        allowed.add(originalNextNodeKey);
        Map<String, List<String>> outgoing = new LinkedHashMap<>();
        Map<String, List<String>> incoming = new LinkedHashMap<>();
        for (WorkflowLinkDefinition link : linkDefinitions) {
            String source = requireText(link.getSourceNodeKey(), "workflow add sign link source must not be blank");
            String target = requireText(link.getTargetNodeKey(), "workflow add sign link target must not be blank");
            if (!allowed.contains(source) || !allowed.contains(target)) {
                throw new PlatformException("workflow add sign link must stay inside local segment: "
                        + source + " -> " + target);
            }
            if (sourceNodeKey.equals(target)) {
                throw new PlatformException("workflow add sign link cannot target source node: " + target);
            }
            if (originalNextNodeKey.equals(source)) {
                throw new PlatformException("workflow add sign link cannot leave original next node: " + source);
            }
            if (sourceNodeKey.equals(source) && originalNextNodeKey.equals(target)) {
                throw new PlatformException("workflow add sign segment cannot be empty direct route");
            }
            outgoing.computeIfAbsent(source, ignored -> new ArrayList<>()).add(target);
            incoming.computeIfAbsent(target, ignored -> new ArrayList<>()).add(source);
        }
        if (outgoing.getOrDefault(sourceNodeKey, List.of()).size() != 1) {
            throw new PlatformException("workflow add sign segment must have one entry from source node");
        }
        if (incoming.getOrDefault(originalNextNodeKey, List.of()).size() != 1) {
            throw new PlatformException("workflow add sign segment must have one exit to original next node");
        }
        for (String addedNodeKey : addedNodeKeys) {
            if (incoming.getOrDefault(addedNodeKey, List.of()).size() != 1
                    || outgoing.getOrDefault(addedNodeKey, List.of()).size() != 1) {
                throw new PlatformException("workflow add sign only supports linear single chain in first version: "
                        + addedNodeKey);
            }
        }
        Set<String> visited = new LinkedHashSet<>();
        String cursor = sourceNodeKey;
        for (int depth = 0; depth <= addedNodeKeys.size(); depth++) {
            List<String> next = outgoing.getOrDefault(cursor, List.of());
            if (next.size() != 1) {
                throw new PlatformException("workflow add sign linear chain is broken at: " + cursor);
            }
            cursor = next.getFirst();
            if (originalNextNodeKey.equals(cursor)) {
                if (visited.size() != addedNodeKeys.size()) {
                    throw new PlatformException("workflow add sign segment must include all added nodes");
                }
                return;
            }
            if (!addedNodeKeys.contains(cursor) || !visited.add(cursor)) {
                throw new PlatformException("workflow add sign segment contains invalid cycle: " + cursor);
            }
        }
        throw new PlatformException("workflow add sign segment must return to original next node: "
                + originalNextNodeKey);
    }

    private void validateAddSignNodeDefinition(WorkflowNodeDefinition definition) {
        if (definition.getNodeType() != WorkflowNodeType.APPROVAL) {
            throw new PlatformException("workflow add sign segment only supports approval nodes in first version: "
                    + definition.getNodeKey());
        }
        String policy = definition.getParticipantPolicyText();
        if (policy == null || policy.isBlank()) {
            throw new PlatformException("workflow add sign approval node participant policy is required: "
                    + definition.getNodeKey());
        }
        String trimmed = policy.trim();
        if (!trimmed.startsWith("user:")) {
            throw new PlatformException("workflow add sign participant policy only supports user:<userId>: "
                    + definition.getNodeKey());
        }
        String userId = trimmed.substring("user:".length()).trim();
        if (userId.isBlank()) {
            throw new PlatformException("workflow add sign participant policy user id must not be blank: "
                    + definition.getNodeKey());
        }
        if (userId.indexOf(',') >= 0 || userId.indexOf(';') >= 0 || userId.contains("[") || userId.contains("]")) {
            throw new PlatformException("workflow add sign participant policy only supports single user in first version: "
                    + definition.getNodeKey());
        }
    }

    private WorkflowNodeInstance addSignNode(WorkflowInstance instance,
                                             WorkflowNodeInstance sourceNode,
                                             WorkflowNodeDefinition definition,
                                             String operatorId,
                                             Instant now) {
        WorkflowNodeInstance node = new WorkflowNodeInstance();
        node.setId(Ids.newId());
        node.setTenantId(instance.getTenantId());
        node.setInstanceId(instance.getId());
        node.setNodeKey(requireText(definition.getNodeKey(), "workflow add sign node key must not be blank"));
        node.setNodeRunId(node.getNodeKey() + ":addSign:1");
        node.setNodeType(definition.getNodeType());
        node.setNodeStatus(WorkflowNodeStatus.WAITING);
        node.setApprovalMode(definition.getApprovalMode());
        node.setApprovalRatio(definition.getApprovalRatio());
        node.setMilestoneType(definition.getMilestoneType());
        node.setConvergeMode(definition.getConvergeMode());
        node.setConvergeRatio(definition.getConvergeRatio());
        node.setTaskDefinitionId(definition.getTaskDefinitionId());
        node.setParticipantPolicyText(definition.getParticipantPolicyText());
        node.setAllowReject(definition.getAllowReject());
        node.setRequireRejectReason(definition.getRequireRejectReason());
        node.setAllowRejectReturnToMe(definition.getAllowRejectReturnToMe());
        node.setAllowRollback(definition.getAllowRollback());
        node.setRequireRollbackReason(definition.getRequireRollbackReason());
        node.setAllowTerminate(definition.getAllowTerminate());
        node.setRequireTerminateReason(definition.getRequireTerminateReason());
        node.setAllowAddSign(definition.getAllowAddSign());
        node.setWarningDurationMinutes(definition.getWarningDurationMinutes());
        node.setOvertimeDurationMinutes(definition.getOvertimeDurationMinutes());
        node.setNodeSnapshotText(definition.getNodeConfigText());
        node.setAddedByAddSign(true);
        node.setAddSignSourceNodeKey(sourceNode.getNodeKey());
        node.setAddSignOperatorId(operatorId);
        node.setAddSignAt(now);
        return node;
    }

    private WorkflowRouteInstance addSignRoute(WorkflowInstance instance,
                                               WorkflowNodeInstance sourceNode,
                                               WorkflowLinkDefinition definition,
                                               String operatorId,
                                               Instant now) {
        WorkflowRouteInstance route = new WorkflowRouteInstance();
        route.setId(Ids.newId());
        route.setTenantId(instance.getTenantId());
        route.setInstanceId(instance.getId());
        route.setRouteKey(requireText(definition.getRouteKey(), "workflow add sign route key must not be blank"));
        route.setRouteRunId(route.getRouteKey() + ":addSign:1");
        route.setSourceNodeKey(requireText(definition.getSourceNodeKey(),
                "workflow add sign route source node key must not be blank"));
        route.setTargetNodeKey(requireText(definition.getTargetNodeKey(),
                "workflow add sign route target node key must not be blank"));
        route.setRouteStatus(WorkflowRouteStatus.CANDIDATE);
        route.setDefaultRoute(definition.getDefaultRoute());
        route.setAddedByAddSign(true);
        route.setAddSignSourceNodeKey(sourceNode.getNodeKey());
        route.setAddSignOperatorId(operatorId);
        route.setAddSignAt(now);
        return route;
    }

    private String addSignPayload(String sourceNodeKey,
                                  List<String> addedNodeKeys,
                                  List<String> replacedRouteIds,
                                  WorkflowAddSignEditMode editMode) {
        return "{\"sourceNodeKey\":\"" + sourceNodeKey
                + "\",\"addedNodeKeys\":" + jsonArray(addedNodeKeys)
                + ",\"replacedRouteIds\":" + jsonArray(replacedRouteIds)
                + ",\"editMode\":\"" + editMode.getCode() + "\"}";
    }

    private String jsonArray(List<String> values) {
        return values == null || values.isEmpty()
                ? "[]"
                : values.stream()
                        .map(value -> "\"" + value.replace("\"", "\\\"") + "\"")
                        .collect(java.util.stream.Collectors.joining(",", "[", "]"));
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

    private void createDelegationCompletionNotice(WorkflowInstance instance, WorkflowTask task,
                                                  String operatorId, Instant now) {
        if (delegationCompletionNoticeService == null) {
            return;
        }
        delegationCompletionNoticeService.createIfNeeded(instance, task, operatorId, now);
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
            throw new PlatformException("workflow route dao is required");
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

    private record AddSignSegmentPlan(List<WorkflowNodeInstance> nodes,
                                      List<WorkflowRouteInstance> routes,
                                      List<String> addedNodeKeys) {
    }
}
