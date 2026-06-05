package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.id.Ids;
import net.ximatai.muyun.spring.common.model.EntityLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkflowTaskActionService {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final WorkflowTaskDao taskDao;
    private final WorkflowInstanceDao instanceDao;
    private final WorkflowNodeInstanceDao nodeInstanceDao;
    private final WorkflowEventDao eventDao;
    private final WorkflowRuntimeEventFactory eventFactory;
    private final WorkflowApprovalTaskPolicyService approvalTaskPolicyService;
    private final WorkflowRuntimeProgressionService progressionService;

    public WorkflowTaskActionService(WorkflowTaskDao taskDao,
                                     WorkflowInstanceDao instanceDao,
                                     WorkflowNodeInstanceDao nodeInstanceDao,
                                     WorkflowEventDao eventDao,
                                     WorkflowRuntimeEventFactory eventFactory,
                                     WorkflowApprovalTaskPolicyService approvalTaskPolicyService,
                                     WorkflowRuntimeProgressionService progressionService) {
        this.taskDao = taskDao;
        this.instanceDao = instanceDao;
        this.nodeInstanceDao = nodeInstanceDao;
        this.eventDao = eventDao;
        this.eventFactory = eventFactory;
        this.approvalTaskPolicyService = approvalTaskPolicyService;
        this.progressionService = progressionService;
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
        task.setTaskStatus(WorkflowTaskStatus.REJECTED);
        task.setActualProcessorId(operatorId);
        task.setDecision("reject");
        task.setResultMessage(request.reason());
        task.setCompletedAt(now);
        updateTask(task, now);

        node.setNodeStatus(WorkflowNodeStatus.REJECTED);
        node.setRejectedTaskCount(value(node.getRejectedTaskCount()) + 1);
        node.setCompletedAt(now);
        updateNode(node, now);

        instance.setInstanceStatus(WorkflowInstanceStatus.REJECTED);
        if (Boolean.TRUE.equals(instance.getApprovalEnabled())) {
            instance.setApprovalStatus(WorkflowApprovalStatus.REJECTED);
        }
        instance.setLastActionCode("reject");
        instance.setLastActionReason(request.reason());
        instance.setLastOperatorId(operatorId);
        instance.setLastOperatedAt(now);
        updateInstance(instance, now);

        WorkflowEvent event = eventFactory.taskRejected(instance, task, operatorId, request.reason(), now);
        eventDao.insert(event);
        return WorkflowTaskActionResult.of(task, node, instance, event);
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
}
