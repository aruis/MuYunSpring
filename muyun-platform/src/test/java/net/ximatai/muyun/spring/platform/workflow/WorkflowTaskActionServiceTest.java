package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WorkflowTaskActionServiceTest {
    private final WorkflowTaskDao taskDao = mock(WorkflowTaskDao.class);
    private final WorkflowInstanceDao instanceDao = mock(WorkflowInstanceDao.class);
    private final WorkflowEventDao eventDao = mock(WorkflowEventDao.class);
    private final WorkflowRuntimeEventFactory eventFactory = new WorkflowRuntimeEventFactory();
    private final WorkflowTaskActionService service = new WorkflowTaskActionService(
            taskDao, instanceDao, eventDao, eventFactory);

    @Test
    void shouldCompleteBusinessTaskAndWriteEvent() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.BUSINESS, WorkflowTaskStatus.TODO);
        WorkflowInstance instance = instance();
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(taskDao.updateByIdAndVersion(task, 3)).thenReturn(1);

        WorkflowTaskActionResult result = service.completeBusinessTask(new WorkflowTaskActionRequest(
                "task-1", "user-1", null, "done", Instant.parse("2026-06-05T02:00:00Z")));

        assertThat(result.task().getTaskStatus()).isEqualTo(WorkflowTaskStatus.DONE);
        assertThat(result.task().getActualProcessorId()).isEqualTo("user-1");
        assertThat(result.task().getDecision()).isEqualTo("complete");
        assertThat(result.task().getCompletedAt()).isEqualTo(Instant.parse("2026-06-05T02:00:00Z"));
        assertThat(result.task().getVersion()).isEqualTo(4);
        assertThat(result.event().getEventType()).isEqualTo(WorkflowEventType.TASK_COMPLETED);
        assertThat(result.event().getActionCode()).isEqualTo("complete");
        verify(taskDao).updateByIdAndVersion(task, 3);
        verify(eventDao).insert(result.event());
    }

    @Test
    void shouldMarkNoticeTaskAsNoticed() {
        WorkflowTask task = task("notice-1", WorkflowTaskKind.NOTICE, WorkflowTaskStatus.TODO);
        when(taskDao.findById("notice-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance());
        when(taskDao.updateByIdAndVersion(task, 3)).thenReturn(1);

        WorkflowTaskActionResult result = service.notice(new WorkflowTaskActionRequest(
                "notice-1", "user-1", null, "read", Instant.parse("2026-06-05T02:00:00Z")));

        assertThat(result.task().getTaskStatus()).isEqualTo(WorkflowTaskStatus.NOTICED);
        assertThat(result.task().getDecision()).isEqualTo("notice");
        assertThat(result.event().getEventType()).isEqualTo(WorkflowEventType.TASK_COMPLETED);
        assertThat(result.event().getActionCode()).isEqualTo("notice");
    }

    @Test
    void shouldTransferTodoTaskByCreatingNewAssigneeTask() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.BUSINESS, WorkflowTaskStatus.TODO);
        task.setAssigneeId("user-a");
        task.setOriginalAssigneeId("user-a");
        task.setOwnerId("owner-1");
        task.setDueAt(Instant.parse("2026-06-06T02:00:00Z"));
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance());
        when(taskDao.updateByIdAndVersion(task, 3)).thenReturn(1);

        WorkflowTaskActionResult result = service.transfer(new WorkflowTaskActionRequest(
                "task-1", "leader-1", "user-b", "handover", Instant.parse("2026-06-05T02:00:00Z")));

        assertThat(result.task().getTaskStatus()).isEqualTo(WorkflowTaskStatus.TRANSFERRED);
        assertThat(result.task().getTransferredBy()).isEqualTo("leader-1");
        assertThat(result.createdTask()).isNotNull();
        assertThat(result.createdTask().getTaskStatus()).isEqualTo(WorkflowTaskStatus.TODO);
        assertThat(result.createdTask().getAssignmentKind()).isEqualTo(WorkflowAssignmentKind.TRANSFERRED);
        assertThat(result.createdTask().getOriginTaskId()).isEqualTo("task-1");
        assertThat(result.createdTask().getAssigneeId()).isEqualTo("user-b");
        assertThat(result.createdTask().getTransferredFromUserId()).isEqualTo("user-a");
        assertThat(result.createdTask().getOriginalAssigneeId()).isEqualTo("user-a");
        assertThat(result.createdTask().getDueAt()).isEqualTo(Instant.parse("2026-06-06T02:00:00Z"));
        assertThat(result.event().getEventType()).isEqualTo(WorkflowEventType.TASK_TRANSFERRED);
        verify(taskDao).insert(result.createdTask());
        verify(eventDao).insert(result.event());
    }

    @Test
    void shouldRejectCompletedTaskAction() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.BUSINESS, WorkflowTaskStatus.DONE);
        when(taskDao.findById("task-1")).thenReturn(task);

        assertThatThrownBy(() -> service.completeBusinessTask(WorkflowTaskActionRequest.complete(
                "task-1", "user-1", null)))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("workflow task is not todo");

        verifyNoInteractions(instanceDao, eventDao);
    }

    @Test
    void shouldRejectBusinessCompleteForApprovalTask() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        when(taskDao.findById("task-1")).thenReturn(task);

        assertThatThrownBy(() -> service.completeBusinessTask(WorkflowTaskActionRequest.complete(
                "task-1", "user-1", null)))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("not a business task");

        verifyNoInteractions(instanceDao, eventDao);
    }

    @Test
    void shouldThrowOptimisticLockWhenTaskUpdateLosesRace() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.BUSINESS, WorkflowTaskStatus.TODO);
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance());
        when(taskDao.updateByIdAndVersion(task, 3)).thenReturn(0);

        assertThatThrownBy(() -> service.completeBusinessTask(WorkflowTaskActionRequest.complete(
                "task-1", "user-1", null)))
                .isInstanceOf(OptimisticLockException.class)
                .hasMessageContaining("workflow task version conflict");

        verifyNoInteractions(eventDao);
    }

    private WorkflowTask task(String id, WorkflowTaskKind kind, WorkflowTaskStatus status) {
        WorkflowTask task = new WorkflowTask();
        task.setId(id);
        task.setTenantId("tenant-1");
        task.setVersion(3);
        task.setInstanceId("instance-1");
        task.setNodeInstanceId("node-1");
        task.setTaskKind(kind);
        task.setTaskStatus(status);
        task.setAssigneeId("user-1");
        task.setCheckStatus(WorkflowTaskCheckStatus.NOT_CHECKED);
        return task;
    }

    private WorkflowInstance instance() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId("instance-1");
        instance.setTenantId("tenant-1");
        return instance;
    }
}
