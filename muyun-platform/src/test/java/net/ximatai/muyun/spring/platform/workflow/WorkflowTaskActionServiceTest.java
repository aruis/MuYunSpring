package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WorkflowTaskActionServiceTest {
    private final WorkflowTaskDao taskDao = mock(WorkflowTaskDao.class);
    private final WorkflowInstanceDao instanceDao = mock(WorkflowInstanceDao.class);
    private final WorkflowNodeInstanceDao nodeDao = mock(WorkflowNodeInstanceDao.class);
    private final WorkflowEventDao eventDao = mock(WorkflowEventDao.class);
    private final WorkflowRuntimeEventFactory eventFactory = new WorkflowRuntimeEventFactory();
    private final WorkflowApprovalTaskPolicyService approvalTaskPolicyService = new WorkflowApprovalTaskPolicyService();
    private final WorkflowRuntimeProgressionService progressionService = mock(WorkflowRuntimeProgressionService.class);
    private final WorkflowTaskActionService service = new WorkflowTaskActionService(
            taskDao, instanceDao, nodeDao, eventDao, eventFactory, approvalTaskPolicyService, progressionService);

    @Test
    void shouldApproveApprovalTaskAndCompleteAnyNode() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowTask sibling = task("task-2", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowNodeInstance node = node(WorkflowApprovalMode.ANY, null);
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance());
        when(nodeDao.findById("node-1")).thenReturn(node);
        when(taskDao.query(any(), any())).thenReturn(List.of(task, sibling));
        when(taskDao.updateByIdAndVersion(task, 3)).thenReturn(1);
        when(taskDao.updateByIdAndVersion(sibling, 3)).thenReturn(1);
        when(nodeDao.updateByIdAndVersion(node, 2)).thenReturn(1);

        WorkflowTaskActionResult result = service.approve(new WorkflowTaskActionRequest(
                "task-1", "user-1", null, "agree", Instant.parse("2026-06-05T02:00:00Z")));

        assertThat(result.task().getTaskStatus()).isEqualTo(WorkflowTaskStatus.DONE);
        assertThat(result.task().getDecision()).isEqualTo("approve");
        assertThat(result.node().getNodeStatus()).isEqualTo(WorkflowNodeStatus.COMPLETED);
        assertThat(result.node().getApprovedTaskCount()).isEqualTo(1);
        assertThat(sibling.getTaskStatus()).isEqualTo(WorkflowTaskStatus.SKIPPED);
        assertThat(sibling.getDecision()).isEqualTo("skip");
        assertThat(result.event().getEventType()).isEqualTo(WorkflowEventType.TASK_COMPLETED);
        assertThat(result.event().getActionCode()).isEqualTo("approve");
        verify(nodeDao).updateByIdAndVersion(node, 2);
        verify(eventDao, atLeastOnce()).insert(any());
        verify(progressionService).advanceFromNode("instance-1", "approve", "user-1",
                Instant.parse("2026-06-05T02:00:00Z"));
    }

    @Test
    void shouldRejectApprovalTaskAndRejectInstance() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowNodeInstance node = node(WorkflowApprovalMode.ALL, null);
        WorkflowInstance instance = instance();
        instance.setApprovalEnabled(true);
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(nodeDao.findById("node-1")).thenReturn(node);
        when(taskDao.updateByIdAndVersion(task, 3)).thenReturn(1);
        when(nodeDao.updateByIdAndVersion(node, 2)).thenReturn(1);
        when(instanceDao.updateByIdAndVersion(instance, 5)).thenReturn(1);

        WorkflowTaskActionResult result = service.reject(new WorkflowTaskActionRequest(
                "task-1", "user-1", null, "not ok", Instant.parse("2026-06-05T02:00:00Z")));

        assertThat(result.task().getTaskStatus()).isEqualTo(WorkflowTaskStatus.REJECTED);
        assertThat(result.node().getNodeStatus()).isEqualTo(WorkflowNodeStatus.REJECTED);
        assertThat(result.instance().getInstanceStatus()).isEqualTo(WorkflowInstanceStatus.REJECTED);
        assertThat(result.instance().getApprovalStatus()).isEqualTo(WorkflowApprovalStatus.REJECTED);
        assertThat(result.instance().getLastActionCode()).isEqualTo("reject");
        assertThat(result.event().getEventType()).isEqualTo(WorkflowEventType.TASK_REJECTED);
        verify(instanceDao).updateByIdAndVersion(instance, 5);
    }

    @Test
    void shouldCompleteBusinessTaskAndWriteEvent() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.BUSINESS, WorkflowTaskStatus.TODO);
        WorkflowInstance instance = instance();
        WorkflowNodeInstance node = node(null, null);
        node.setNodeType(WorkflowNodeType.TASK);
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(nodeDao.findById("node-1")).thenReturn(node);
        when(taskDao.updateByIdAndVersion(task, 3)).thenReturn(1);
        when(nodeDao.updateByIdAndVersion(node, 2)).thenReturn(1);

        WorkflowTaskActionResult result = service.completeBusinessTask(new WorkflowTaskActionRequest(
                "task-1", "user-1", null, "done", Instant.parse("2026-06-05T02:00:00Z")));

        assertThat(result.task().getTaskStatus()).isEqualTo(WorkflowTaskStatus.DONE);
        assertThat(result.task().getActualProcessorId()).isEqualTo("user-1");
        assertThat(result.task().getDecision()).isEqualTo("complete");
        assertThat(result.task().getCompletedAt()).isEqualTo(Instant.parse("2026-06-05T02:00:00Z"));
        assertThat(result.task().getVersion()).isEqualTo(4);
        assertThat(result.node().getNodeStatus()).isEqualTo(WorkflowNodeStatus.COMPLETED);
        assertThat(result.event().getEventType()).isEqualTo(WorkflowEventType.TASK_COMPLETED);
        assertThat(result.event().getActionCode()).isEqualTo("complete");
        verify(taskDao).updateByIdAndVersion(task, 3);
        verify(nodeDao).updateByIdAndVersion(node, 2);
        verify(eventDao).insert(result.event());
        verify(progressionService).advanceFromNode("instance-1", "approve", "user-1",
                Instant.parse("2026-06-05T02:00:00Z"));
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
        WorkflowNodeInstance node = node(null, null);
        node.setNodeType(WorkflowNodeType.TASK);
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance());
        when(nodeDao.findById("node-1")).thenReturn(node);
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

    private WorkflowNodeInstance node(WorkflowApprovalMode mode, Integer ratio) {
        WorkflowNodeInstance node = new WorkflowNodeInstance();
        node.setId("node-1");
        node.setTenantId("tenant-1");
        node.setVersion(2);
        node.setInstanceId("instance-1");
        node.setNodeKey("approve");
        node.setNodeType(WorkflowNodeType.APPROVAL);
        node.setNodeStatus(WorkflowNodeStatus.ACTIVE);
        node.setApprovalMode(mode);
        node.setApprovalRatio(ratio);
        return node;
    }

    private WorkflowInstance instance() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId("instance-1");
        instance.setTenantId("tenant-1");
        instance.setVersion(5);
        instance.setInstanceStatus(WorkflowInstanceStatus.RUNNING);
        instance.setApprovalStatus(WorkflowApprovalStatus.PROCESSING);
        return instance;
    }
}
