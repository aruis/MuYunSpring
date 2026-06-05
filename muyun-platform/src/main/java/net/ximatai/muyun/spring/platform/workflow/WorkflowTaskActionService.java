package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.id.Ids;
import net.ximatai.muyun.spring.common.model.EntityLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class WorkflowTaskActionService {
    private final WorkflowTaskDao taskDao;
    private final WorkflowInstanceDao instanceDao;
    private final WorkflowEventDao eventDao;
    private final WorkflowRuntimeEventFactory eventFactory;

    public WorkflowTaskActionService(WorkflowTaskDao taskDao,
                                     WorkflowInstanceDao instanceDao,
                                     WorkflowEventDao eventDao,
                                     WorkflowRuntimeEventFactory eventFactory) {
        this.taskDao = taskDao;
        this.instanceDao = instanceDao;
        this.eventDao = eventDao;
        this.eventFactory = eventFactory;
    }

    @Transactional
    public WorkflowTaskActionResult completeBusinessTask(WorkflowTaskActionRequest request) {
        WorkflowTask task = requireTodoTask(request);
        if (task.getTaskKind() != WorkflowTaskKind.BUSINESS) {
            throw new PlatformException("workflow task is not a business task: " + request.taskId());
        }
        WorkflowInstance instance = requireInstance(task);
        Instant now = operatedAt(request);
        String operatorId = operatorId(request);
        task.setTaskStatus(WorkflowTaskStatus.DONE);
        task.setActualProcessorId(operatorId);
        task.setDecision("complete");
        task.setResultMessage(request.reason());
        task.setCompletedAt(now);
        updateTask(task, now);
        WorkflowEvent event = eventFactory.taskCompleted(instance, task, "complete", operatorId,
                request.reason(), now);
        eventDao.insert(event);
        return WorkflowTaskActionResult.of(task, event);
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

    private void updateTask(WorkflowTask task, Instant now) {
        Integer expectedVersion = task.getVersion();
        EntityLifecycle.prepareUpdate(task, now, EntityLifecycle.nextVersion(expectedVersion));
        int updated = taskDao.updateByIdAndVersion(task, expectedVersion);
        if (updated <= 0) {
            throw new OptimisticLockException("workflow task version conflict: " + task.getId());
        }
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
