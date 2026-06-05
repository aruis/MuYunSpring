package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class WorkflowTaskActionServiceTest {
    private final WorkflowTaskDao taskDao = mock(WorkflowTaskDao.class);
    private final WorkflowInstanceDao instanceDao = mock(WorkflowInstanceDao.class);
    private final WorkflowNodeInstanceDao nodeDao = mock(WorkflowNodeInstanceDao.class);
    private final WorkflowRouteInstanceDao routeDao = mock(WorkflowRouteInstanceDao.class);
    private final WorkflowEventDao eventDao = mock(WorkflowEventDao.class);
    private final WorkflowRuntimeEventFactory eventFactory = new WorkflowRuntimeEventFactory();
    private final WorkflowApprovalTaskPolicyService approvalTaskPolicyService = new WorkflowApprovalTaskPolicyService();
    private final WorkflowActionPolicyService actionPolicyService = new WorkflowActionPolicyService();
    private final WorkflowRuntimeProgressionService progressionService = mock(WorkflowRuntimeProgressionService.class);
    private final WorkflowTaskActionService service = new WorkflowTaskActionService(
            taskDao, instanceDao, nodeDao, routeDao, eventDao, eventFactory, approvalTaskPolicyService,
            actionPolicyService, progressionService, Optional.empty());

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
                "task-1", "user-1", null, null, "agree", Instant.parse("2026-06-05T02:00:00Z")));

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
    void shouldRequireRuntimeAuthorizationBeforeTaskAction() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowInstance instance = instance();
        WorkflowNodeInstance node = node(WorkflowApprovalMode.ANY, null);
        WorkflowActionPolicyService policyService = mock(WorkflowActionPolicyService.class);
        WorkflowTaskActionService authorizedService = new WorkflowTaskActionService(
                taskDao, instanceDao, nodeDao, routeDao, eventDao, eventFactory, approvalTaskPolicyService,
                policyService, progressionService, Optional.empty());
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(nodeDao.findById("node-1")).thenReturn(node);
        when(taskDao.query(any(), any())).thenReturn(List.of(task));
        when(taskDao.updateByIdAndVersion(task, 3)).thenReturn(1);
        when(nodeDao.updateByIdAndVersion(node, 2)).thenReturn(1);

        authorizedService.approve(WorkflowTaskActionRequest.complete("task-1", "user-1", "agree"));

        verify(policyService).requireRuntimeAction(instance, "approve");
        verify(policyService).requireTaskOperator(task, "approve", "user-1");
    }

    @Test
    void shouldForceApproveByAdminWithoutAssigneeCheck() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowNodeInstance node = node(WorkflowApprovalMode.ANY, null);
        WorkflowActionPolicyService policyService = mock(WorkflowActionPolicyService.class);
        WorkflowTaskActionService adminService = new WorkflowTaskActionService(
                taskDao, instanceDao, nodeDao, routeDao, eventDao, eventFactory, approvalTaskPolicyService,
                policyService, progressionService, Optional.empty());
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance());
        when(nodeDao.findById("node-1")).thenReturn(node);
        when(taskDao.query(any(), any())).thenReturn(List.of(task));
        when(taskDao.updateByIdAndVersion(task, 3)).thenReturn(1);
        when(nodeDao.updateByIdAndVersion(node, 2)).thenReturn(1);

        WorkflowTaskActionResult result = adminService.forceApprove(new WorkflowTaskActionRequest(
                "task-1", "admin-1", null, null, "admin approved", Instant.parse("2026-06-05T02:30:00Z")));

        assertThat(result.task().getTaskStatus()).isEqualTo(WorkflowTaskStatus.DONE);
        assertThat(result.task().getActualProcessorId()).isEqualTo("admin-1");
        assertThat(result.task().getDecision()).isEqualTo("forceApprove");
        assertThat(result.node().getNodeStatus()).isEqualTo(WorkflowNodeStatus.COMPLETED);
        assertThat(result.event().getActionCode()).isEqualTo("forceApprove");
        verify(policyService).requireRuntimeAction(result.instance(), "forceApprove");
        verify(policyService).requireManagementTaskAction("forceApprove", "admin approved");
        verify(progressionService).advanceFromNode("instance-1", "approve", "admin-1",
                Instant.parse("2026-06-05T02:30:00Z"));
    }

    @Test
    void shouldRejectForceApproveForBusinessTask() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.BUSINESS, WorkflowTaskStatus.TODO);
        when(taskDao.findById("task-1")).thenReturn(task);

        assertThatThrownBy(() -> service.forceApprove(new WorkflowTaskActionRequest(
                "task-1", "admin-1", null, null, "admin approved", Instant.parse("2026-06-05T02:30:00Z"))))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("only supports approval task");

        verifyNoInteractions(instanceDao, nodeDao, eventDao);
    }

    @Test
    void shouldRejectApprovalTaskAndCreateRestartResubmitTask() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowTask sibling = task("task-2", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowNodeInstance node = node(WorkflowApprovalMode.ALL, null);
        WorkflowInstance instance = instance();
        instance.setApprovalEnabled(true);
        instance.setStartedBy("starter-1");
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(nodeDao.findById("node-1")).thenReturn(node);
        when(taskDao.query(any(), any())).thenReturn(List.of(task, sibling));
        when(taskDao.updateByIdAndVersion(task, 3)).thenReturn(1);
        when(taskDao.updateByIdAndVersion(sibling, 3)).thenReturn(1);
        when(nodeDao.updateByIdAndVersion(node, 2)).thenReturn(1);
        when(instanceDao.updateByIdAndVersion(instance, 5)).thenReturn(1);

        WorkflowTaskActionResult result = service.reject(new WorkflowTaskActionRequest(
                "task-1", "user-1", null, null, "not ok", Instant.parse("2026-06-05T02:00:00Z")));

        assertThat(result.task().getTaskStatus()).isEqualTo(WorkflowTaskStatus.REJECTED);
        assertThat(sibling.getTaskStatus()).isEqualTo(WorkflowTaskStatus.CANCELED);
        assertThat(result.node().getNodeStatus()).isEqualTo(WorkflowNodeStatus.REJECTED);
        assertThat(result.instance().getInstanceStatus()).isEqualTo(WorkflowInstanceStatus.REJECTED);
        assertThat(result.instance().getApprovalStatus()).isEqualTo(WorkflowApprovalStatus.REJECTED);
        assertThat(result.instance().getRejectResubmitMode()).isEqualTo(WorkflowRejectResubmitMode.RESTART);
        assertThat(result.instance().getRejectReturnNodeKey()).isNull();
        assertThat(result.instance().getLastActionCode()).isEqualTo("reject");
        assertThat(result.createdTask()).isNotNull();
        assertThat(result.createdTask().getTaskKind()).isEqualTo(WorkflowTaskKind.RESUBMIT);
        assertThat(result.createdTask().getAssigneeId()).isEqualTo("starter-1");
        assertThat(result.event().getEventType()).isEqualTo(WorkflowEventType.TASK_REJECTED);
        verify(instanceDao).updateByIdAndVersion(instance, 5);
        verify(taskDao).insert(result.createdTask());
    }

    @Test
    void shouldRejectApprovalTaskWithReturnToMeMetadata() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        task.setAssigneeId("manager-1");
        WorkflowNodeInstance node = node(WorkflowApprovalMode.ALL, null);
        node.setAllowRejectReturnToMe(true);
        WorkflowInstance instance = instance();
        instance.setApprovalEnabled(true);
        instance.setStartedBy("starter-1");
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(nodeDao.findById("node-1")).thenReturn(node);
        when(taskDao.query(any(), any())).thenReturn(List.of(task));
        when(taskDao.updateByIdAndVersion(task, 3)).thenReturn(1);
        when(nodeDao.updateByIdAndVersion(node, 2)).thenReturn(1);
        when(instanceDao.updateByIdAndVersion(instance, 5)).thenReturn(1);

        WorkflowTaskActionResult result = service.reject(new WorkflowTaskActionRequest(
                "task-1", "manager-1", null, WorkflowRejectResubmitMode.RETURN_TO_ME,
                "not ok", Instant.parse("2026-06-05T02:00:00Z")));

        assertThat(result.instance().getRejectResubmitMode()).isEqualTo(WorkflowRejectResubmitMode.RETURN_TO_ME);
        assertThat(result.instance().getRejectReturnNodeKey()).isEqualTo("approve");
        assertThat(result.instance().getRejectReturnOwnerId()).isEqualTo("manager-1");
        assertThat(result.createdTask().getDueAt()).isNull();
        assertThat(result.createdTask().getTaskKind()).isEqualTo(WorkflowTaskKind.RESUBMIT);
    }

    @Test
    void shouldRejectReturnToMeWhenNodeDoesNotAllowIt() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowNodeInstance node = node(WorkflowApprovalMode.ALL, null);
        node.setAllowRejectReturnToMe(false);
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance());
        when(nodeDao.findById("node-1")).thenReturn(node);

        assertThatThrownBy(() -> service.reject(WorkflowTaskActionRequest.reject(
                "task-1", "user-1", WorkflowRejectResubmitMode.RETURN_TO_ME, "not ok")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("does not allow reject return to me");

        verifyNoInteractions(eventDao);
    }

    @Test
    void shouldRejectApprovalRejectWhenNodeDoesNotAllowIt() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowNodeInstance node = node(WorkflowApprovalMode.ALL, null);
        node.setAllowReject(false);
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance());
        when(nodeDao.findById("node-1")).thenReturn(node);

        assertThatThrownBy(() -> service.reject(WorkflowTaskActionRequest.reject(
                "task-1", "user-1", WorkflowRejectResubmitMode.RESTART, "not ok")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("does not allow reject");

        verifyNoInteractions(eventDao);
    }

    @Test
    void shouldResubmitReturnToRejectOwnerInSameInstance() {
        WorkflowTask task = task("resubmit-1", WorkflowTaskKind.RESUBMIT, WorkflowTaskStatus.TODO);
        task.setAssigneeId("starter-1");
        task.setNodeInstanceId(null);
        WorkflowInstance instance = instance();
        instance.setInstanceStatus(WorkflowInstanceStatus.REJECTED);
        instance.setApprovalEnabled(true);
        instance.setRejectResubmitMode(WorkflowRejectResubmitMode.RETURN_TO_ME);
        instance.setRejectReturnNodeKey("approve");
        instance.setRejectReturnOwnerId("manager-1");
        WorkflowNodeInstance node = node(WorkflowApprovalMode.ALL, null);
        node.setNodeStatus(WorkflowNodeStatus.REJECTED);
        when(taskDao.findById("resubmit-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(nodeDao.query(any(), any())).thenReturn(List.of(node));
        when(taskDao.updateByIdAndVersion(task, 3)).thenReturn(1);
        when(nodeDao.updateByIdAndVersion(node, 2)).thenReturn(1);
        when(instanceDao.updateByIdAndVersion(instance, 5)).thenReturn(1);

        WorkflowTaskActionResult result = service.resubmit(new WorkflowTaskActionRequest(
                "resubmit-1", "starter-1", null, null, "fixed", Instant.parse("2026-06-05T03:00:00Z")));

        assertThat(task.getTaskStatus()).isEqualTo(WorkflowTaskStatus.DONE);
        assertThat(instance.getInstanceStatus()).isEqualTo(WorkflowInstanceStatus.RUNNING);
        assertThat(instance.getApprovalStatus()).isEqualTo(WorkflowApprovalStatus.PROCESSING);
        assertThat(instance.getCurrentNodeKeys()).isEqualTo("approve");
        assertThat(node.getNodeStatus()).isEqualTo(WorkflowNodeStatus.ACTIVE);
        assertThat(result.createdTask()).isNotNull();
        assertThat(result.createdTask().getTaskKind()).isEqualTo(WorkflowTaskKind.APPROVAL);
        assertThat(result.createdTask().getAssigneeId()).isEqualTo("manager-1");
        assertThat(result.createdTask().getDueAt()).isNull();
        assertThat(result.event().getEventType()).isEqualTo(WorkflowEventType.TASK_RESUBMITTED);
        verify(taskDao).insert(result.createdTask());
    }

    @Test
    void shouldRejectRestartResubmitInTaskActionEntry() {
        WorkflowTask task = task("resubmit-1", WorkflowTaskKind.RESUBMIT, WorkflowTaskStatus.TODO);
        WorkflowInstance instance = instance();
        instance.setInstanceStatus(WorkflowInstanceStatus.REJECTED);
        instance.setRejectResubmitMode(WorkflowRejectResubmitMode.RESTART);
        when(taskDao.findById("resubmit-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance);

        assertThatThrownBy(() -> service.resubmit(new WorkflowTaskActionRequest(
                "resubmit-1", "starter-1", null, null, "fixed", Instant.parse("2026-06-05T03:00:00Z"))))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("restart resubmit requires new workflow submit entry");

        verifyNoInteractions(nodeDao, eventDao);
    }

    @Test
    void shouldRollbackToPreviousLinearApprovalNode() {
        WorkflowTask currentTask = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowTask previousDoneTask = task("task-prev", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.DONE);
        previousDoneTask.setNodeInstanceId("node-prev");
        previousDoneTask.setActualProcessorId("leader-1");
        WorkflowInstance instance = instance();
        instance.setApprovalEnabled(true);
        WorkflowNodeInstance currentNode = node(WorkflowApprovalMode.ALL, null);
        WorkflowNodeInstance previousNode = node("node-prev", "leader", WorkflowNodeType.APPROVAL,
                WorkflowNodeStatus.COMPLETED);
        WorkflowRouteInstance route = route("route-1", "leader", "approve", WorkflowRouteStatus.EFFECTIVE);
        when(taskDao.findById("task-1")).thenReturn(currentTask);
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(nodeDao.findById("node-1")).thenReturn(currentNode);
        when(taskDao.query(any(), any())).thenReturn(List.of(currentTask), List.of(currentTask),
                List.of(previousDoneTask));
        when(routeDao.query(any(), any())).thenReturn(List.of(route));
        when(nodeDao.query(any(), any())).thenReturn(List.of(previousNode));
        when(taskDao.updateByIdAndVersion(currentTask, 3)).thenReturn(1);
        when(nodeDao.updateByIdAndVersion(currentNode, 2)).thenReturn(1);
        when(nodeDao.updateByIdAndVersion(previousNode, 2)).thenReturn(1);
        when(routeDao.updateByIdAndVersion(route, 4)).thenReturn(1);
        when(instanceDao.updateByIdAndVersion(instance, 5)).thenReturn(1);

        WorkflowTaskActionResult result = service.rollback(new WorkflowTaskActionRequest(
                "task-1", "user-1", null, null, "return", Instant.parse("2026-06-05T03:00:00Z")));

        assertThat(currentTask.getTaskStatus()).isEqualTo(WorkflowTaskStatus.ROLLED_BACK);
        assertThat(currentNode.getNodeStatus()).isEqualTo(WorkflowNodeStatus.ROLLED_BACK);
        assertThat(currentNode.getRollbackTargetNodeKey()).isEqualTo("leader");
        assertThat(previousNode.getNodeStatus()).isEqualTo(WorkflowNodeStatus.ACTIVE);
        assertThat(route.getRouteStatus()).isEqualTo(WorkflowRouteStatus.CANDIDATE);
        assertThat(instance.getCurrentNodeKeys()).isEqualTo("leader");
        assertThat(result.createdTask()).isNotNull();
        assertThat(result.createdTask().getAssigneeId()).isEqualTo("leader-1");
        assertThat(result.createdTask().getDueAt()).isNull();
        assertThat(result.event().getEventType()).isEqualTo(WorkflowEventType.NODE_ROLLED_BACK);
    }

    @Test
    void shouldRejectRollbackWhenPreviousApprovalNodeIsNotFound() {
        WorkflowTask currentTask = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowInstance instance = instance();
        WorkflowNodeInstance currentNode = node(WorkflowApprovalMode.ALL, null);
        when(taskDao.findById("task-1")).thenReturn(currentTask);
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(nodeDao.findById("node-1")).thenReturn(currentNode);
        when(routeDao.query(any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.rollback(WorkflowTaskActionRequest.complete(
                "task-1", "user-1", "return")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("previous approval node not found");
    }

    @Test
    void shouldRejectRollbackWhenPreviousApprovalNodeIsNotUnique() {
        WorkflowTask currentTask = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowInstance instance = instance();
        WorkflowNodeInstance currentNode = node(WorkflowApprovalMode.ALL, null);
        WorkflowRouteInstance first = route("route-1", "leader", "approve", WorkflowRouteStatus.EFFECTIVE);
        WorkflowRouteInstance second = route("route-2", "finance", "approve", WorkflowRouteStatus.EFFECTIVE);
        when(taskDao.findById("task-1")).thenReturn(currentTask);
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(nodeDao.findById("node-1")).thenReturn(currentNode);
        when(routeDao.query(any(), any())).thenReturn(List.of(first, second));

        assertThatThrownBy(() -> service.rollback(WorkflowTaskActionRequest.complete(
                "task-1", "user-1", "return")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("linear effective route");
    }

    @Test
    void shouldRejectRollbackWhenAnotherNodeIsStillActive() {
        WorkflowTask currentTask = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowTask parallelTask = task("task-2", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        parallelTask.setNodeInstanceId("node-parallel");
        WorkflowInstance instance = instance();
        WorkflowNodeInstance currentNode = node(WorkflowApprovalMode.ALL, null);
        WorkflowNodeInstance previousNode = node("node-prev", "leader", WorkflowNodeType.APPROVAL,
                WorkflowNodeStatus.COMPLETED);
        WorkflowRouteInstance route = route("route-1", "leader", "approve", WorkflowRouteStatus.EFFECTIVE);
        when(taskDao.findById("task-1")).thenReturn(currentTask);
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(nodeDao.findById("node-1")).thenReturn(currentNode);
        when(routeDao.query(any(), any())).thenReturn(List.of(route));
        when(nodeDao.query(any(), any())).thenReturn(List.of(previousNode));
        when(taskDao.query(any(), any())).thenReturn(List.of(currentTask, parallelTask));

        assertThatThrownBy(() -> service.rollback(WorkflowTaskActionRequest.complete(
                "task-1", "user-1", "return")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("single active node");

        verifyNoInteractions(eventDao);
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
                "task-1", "user-1", null, null, "done", Instant.parse("2026-06-05T02:00:00Z")));

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
                "notice-1", "user-1", null, null, "read", Instant.parse("2026-06-05T02:00:00Z")));

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
                "task-1", "user-a", "user-b", null, "handover", Instant.parse("2026-06-05T02:00:00Z")));

        assertThat(result.task().getTaskStatus()).isEqualTo(WorkflowTaskStatus.TRANSFERRED);
        assertThat(result.task().getTransferredBy()).isEqualTo("user-a");
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
    void shouldInsertRuntimeAddSignSegmentWithoutCompletingCurrentTask() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowInstance instance = instance();
        WorkflowNodeInstance node = node(WorkflowApprovalMode.ALL, null);
        node.setAllowAddSign(true);
        WorkflowRouteInstance originalRoute = route("route-1", "approve", "next", WorkflowRouteStatus.CANDIDATE);
        originalRoute.setRouteKey("approve-next");
        originalRoute.setDefaultRoute(true);
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(nodeDao.findById("node-1")).thenReturn(node);
        when(taskDao.query(any(), any())).thenReturn(List.of(task), List.of());
        when(routeDao.query(any(), any())).thenReturn(List.of(originalRoute));
        when(nodeDao.query(any(), any())).thenReturn(List.of(), List.of(node, node("node-next", "next",
                WorkflowNodeType.END, WorkflowNodeStatus.WAITING)));
        when(routeDao.updateByIdAndVersion(originalRoute, 4)).thenReturn(1);

        WorkflowTaskActionResult result = service.addSign(new WorkflowTaskActionRequest(
                "task-1", "user-1", null, null, segment("add-1", "approve", "next"), null,
                "need finance review", Instant.parse("2026-06-05T04:00:00Z")));

        assertThat(result.task().getTaskStatus()).isEqualTo(WorkflowTaskStatus.TODO);
        assertThat(result.node().getNodeStatus()).isEqualTo(WorkflowNodeStatus.ACTIVE);
        assertThat(result.createdTask()).isNull();
        assertThat(result.addSignEditMode()).isEqualTo(WorkflowAddSignEditMode.CREATE);
        assertThat(result.sourceNodeKey()).isEqualTo("approve");
        assertThat(result.addedNodeKeys()).containsExactly("add-1");
        assertThat(result.replacedRouteIds()).containsExactly("route-1");
        assertThat(originalRoute.getRouteStatus()).isEqualTo(WorkflowRouteStatus.CANCELED);
        assertThat(originalRoute.getClosedReason()).isEqualTo("workflow route replaced by addSign");
        ArgumentCaptor<WorkflowNodeInstance> nodeCaptor = ArgumentCaptor.forClass(WorkflowNodeInstance.class);
        verify(nodeDao).insert(nodeCaptor.capture());
        assertThat(nodeCaptor.getValue().getNodeKey()).isEqualTo("add-1");
        assertThat(nodeCaptor.getValue().getNodeStatus()).isEqualTo(WorkflowNodeStatus.WAITING);
        assertThat(nodeCaptor.getValue().getParticipantPolicyText()).isEqualTo("user:add-signer-1");
        assertThat(nodeCaptor.getValue().getAddedByAddSign()).isTrue();
        assertThat(nodeCaptor.getValue().getAddSignSourceNodeKey()).isEqualTo("approve");
        assertThat(nodeCaptor.getValue().getAddSignOperatorId()).isEqualTo("user-1");
        assertThat(nodeCaptor.getValue().getAddSignAt()).isEqualTo(Instant.parse("2026-06-05T04:00:00Z"));
        ArgumentCaptor<WorkflowRouteInstance> routeCaptor = ArgumentCaptor.forClass(WorkflowRouteInstance.class);
        verify(routeDao, times(2)).insert(routeCaptor.capture());
        assertThat(routeCaptor.getAllValues()).extracting(WorkflowRouteInstance::getSourceNodeKey)
                .containsExactly("approve", "add-1");
        assertThat(routeCaptor.getAllValues()).extracting(WorkflowRouteInstance::getTargetNodeKey)
                .containsExactly("add-1", "next");
        assertThat(routeCaptor.getAllValues()).extracting(WorkflowRouteInstance::getRouteStatus)
                .containsOnly(WorkflowRouteStatus.CANDIDATE);
        assertThat(result.event().getEventType()).isEqualTo(WorkflowEventType.ADD_SIGN);
        assertThat(result.event().getActionCode()).isEqualTo("addSign");
        assertThat(result.event().getTaskId()).isEqualTo("task-1");
        assertThat(result.event().getPayloadText()).contains("\"sourceNodeKey\":\"approve\"");
        assertThat(result.event().getPayloadText()).contains("\"addedNodeKeys\":[\"add-1\"]");
        assertThat(result.event().getPayloadText()).contains("\"replacedRouteIds\":[\"route-1\"]");
        verify(taskDao, never()).insert(any());
        verify(eventDao).insert(result.event());
        verifyNoInteractions(progressionService);
    }

    @Test
    void shouldRequireRuntimeAuthorizationBeforeAddSign() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowInstance instance = instance();
        WorkflowNodeInstance node = node(WorkflowApprovalMode.ALL, null);
        node.setAllowAddSign(true);
        WorkflowActionPolicyService policyService = mock(WorkflowActionPolicyService.class);
        WorkflowTaskActionService authorizedService = new WorkflowTaskActionService(
                taskDao, instanceDao, nodeDao, routeDao, eventDao, eventFactory, approvalTaskPolicyService,
                policyService, progressionService, Optional.empty());
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(nodeDao.findById("node-1")).thenReturn(node);
        when(taskDao.query(any(), any())).thenReturn(List.of(task), List.of());
        when(routeDao.query(any(), any())).thenReturn(List.of(route("route-1", "approve", "next",
                WorkflowRouteStatus.CANDIDATE)));
        when(nodeDao.query(any(), any())).thenReturn(List.of(), List.of(node, node("node-next", "next",
                WorkflowNodeType.END, WorkflowNodeStatus.WAITING)));
        when(routeDao.updateByIdAndVersion(any(), any())).thenReturn(1);

        authorizedService.addSign(WorkflowTaskActionRequest.addSign(
                "task-1", "user-1", segment("add-1", "approve", "next"), "need review"));

        verify(policyService).requireRuntimeAction(instance, "addSign");
        verify(policyService).requireNodeTaskAction(task, node, "addSign", "user-1", "need review");
    }

    @Test
    void shouldRejectAddSignWhenNodeDoesNotAllowIt() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowNodeInstance node = node(WorkflowApprovalMode.ALL, null);
        node.setAllowAddSign(false);
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance());
        when(nodeDao.findById("node-1")).thenReturn(node);

        assertThatThrownBy(() -> service.addSign(WorkflowTaskActionRequest.addSign(
                "task-1", "user-1", segment("add-1", "approve", "next"), "need review")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("does not allow add sign");

        verifyNoInteractions(eventDao);
    }

    @Test
    void shouldRejectAddSignForNonApprovalTask() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.BUSINESS, WorkflowTaskStatus.TODO);
        when(taskDao.findById("task-1")).thenReturn(task);

        assertThatThrownBy(() -> service.addSign(WorkflowTaskActionRequest.addSign(
                "task-1", "user-1", segment("add-1", "approve", "next"), "need review")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("only supports approval task");

        verifyNoInteractions(instanceDao, nodeDao, eventDao);
    }

    @Test
    void shouldRejectAddSignWhenNodeHasMultipleTodoApprovalTasks() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowTask sibling = task("task-2", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowNodeInstance node = node(WorkflowApprovalMode.ALL, null);
        node.setAllowAddSign(true);
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance());
        when(nodeDao.findById("node-1")).thenReturn(node);
        when(taskDao.query(any(), any())).thenReturn(List.of(task, sibling));

        assertThatThrownBy(() -> service.addSign(WorkflowTaskActionRequest.addSign(
                "task-1", "user-1", segment("add-1", "approve", "next"), "need review")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("multi todo approval tasks");

        verifyNoInteractions(eventDao);
    }

    @Test
    void shouldRejectInvalidAddSignSegments() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowInstance instance = instance();
        WorkflowNodeInstance node = node(WorkflowApprovalMode.ALL, null);
        node.setAllowAddSign(true);
        WorkflowRouteInstance originalRoute = route("route-1", "approve", "next", WorkflowRouteStatus.CANDIDATE);
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(nodeDao.findById("node-1")).thenReturn(node);
        when(taskDao.query(any(), any())).thenReturn(List.of(task), List.of());
        when(routeDao.query(any(), any())).thenReturn(List.of(originalRoute));
        when(nodeDao.query(any(), any())).thenReturn(
                List.of(),
                List.of(),
                List.of(node, node("node-next", "next", WorkflowNodeType.END, WorkflowNodeStatus.WAITING)),
                List.of(),
                List.of(node, node("node-next", "next", WorkflowNodeType.END, WorkflowNodeStatus.WAITING)),
                List.of(),
                List.of(node, node("node-next", "next", WorkflowNodeType.END, WorkflowNodeStatus.WAITING)),
                List.of(),
                List.of(node, node("node-next", "next", WorkflowNodeType.END, WorkflowNodeStatus.WAITING)),
                List.of(),
                List.of(node, node("node-next", "next", WorkflowNodeType.END, WorkflowNodeStatus.WAITING)),
                List.of(),
                List.of(node, node("node-next", "next", WorkflowNodeType.END, WorkflowNodeStatus.WAITING)));

        assertThatThrownBy(() -> service.addSign(WorkflowTaskActionRequest.addSign(
                "task-1", "user-1", new WorkflowAddSignSegment(List.of(), List.of()), "need review")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("must contain nodes");
        assertThatThrownBy(() -> service.addSign(WorkflowTaskActionRequest.addSign(
                "task-1", "user-1",
                new WorkflowAddSignSegment(List.of(nodeDefinition("add-1")), List.of(
                        linkDefinition("direct", "approve", "next"))), "need review")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("empty direct route");
        assertThatThrownBy(() -> service.addSign(WorkflowTaskActionRequest.addSign(
                "task-1", "user-1",
                new WorkflowAddSignSegment(List.of(nodeDefinition("add-1")), List.of(
                        linkDefinition("entry", "approve", "add-1"))), "need review")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("one exit");
        assertThatThrownBy(() -> service.addSign(WorkflowTaskActionRequest.addSign(
                "task-1", "user-1", segment("approve", "approve", "next"), "need review")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("conflicts");
        assertThatThrownBy(() -> service.addSign(WorkflowTaskActionRequest.addSign(
                "task-1", "user-1",
                new WorkflowAddSignSegment(List.of(nodeDefinition("add-missing", null)), List.of(
                        linkDefinition("entry-missing", "approve", "add-missing"),
                        linkDefinition("exit-missing", "add-missing", "next"))), "need review")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("participant policy is required");
        assertThatThrownBy(() -> service.addSign(WorkflowTaskActionRequest.addSign(
                "task-1", "user-1",
                new WorkflowAddSignSegment(List.of(nodeDefinition("add-invalid", "role:finance")), List.of(
                        linkDefinition("entry-invalid", "approve", "add-invalid"),
                        linkDefinition("exit-invalid", "add-invalid", "next"))), "need review")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("only supports user:<userId>");
        assertThatThrownBy(() -> service.addSign(WorkflowTaskActionRequest.addSign(
                "task-1", "user-1",
                new WorkflowAddSignSegment(List.of(nodeDefinition("add-task", WorkflowNodeType.TASK,
                        "user:add-signer-1")), List.of(
                        linkDefinition("entry-task", "approve", "add-task"),
                        linkDefinition("exit-task", "add-task", "next"))), "need review")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("only supports approval nodes");
    }

    @Test
    void shouldRejectTaskActionWhenOperatorIsNotAssignee() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowNodeInstance node = node(WorkflowApprovalMode.ALL, null);
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance());
        when(nodeDao.findById("node-1")).thenReturn(node);

        assertThatThrownBy(() -> service.approve(WorkflowTaskActionRequest.complete(
                "task-1", "other-user", "agree")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("operator is not assignee");

        verifyNoInteractions(eventDao);
    }

    @Test
    void shouldRequireReasonForRiskTaskAction() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowNodeInstance node = node(WorkflowApprovalMode.ALL, null);
        node.setRequireRejectReason(true);
        when(taskDao.findById("task-1")).thenReturn(task);
        when(instanceDao.findById("instance-1")).thenReturn(instance());
        when(nodeDao.findById("node-1")).thenReturn(node);

        assertThatThrownBy(() -> service.reject(WorkflowTaskActionRequest.reject(
                "task-1", "user-1", WorkflowRejectResubmitMode.RESTART, null)))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("reason is required");

        verifyNoInteractions(eventDao);
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

    private WorkflowAddSignSegment segment(String addedNodeKey, String sourceNodeKey, String nextNodeKey) {
        return new WorkflowAddSignSegment(List.of(nodeDefinition(addedNodeKey)), List.of(
                linkDefinition("entry-" + addedNodeKey, sourceNodeKey, addedNodeKey),
                linkDefinition("exit-" + addedNodeKey, addedNodeKey, nextNodeKey)
        ));
    }

    private WorkflowNodeDefinition nodeDefinition(String nodeKey) {
        return nodeDefinition(nodeKey, "user:add-signer-1");
    }

    private WorkflowNodeDefinition nodeDefinition(String nodeKey, String participantPolicyText) {
        return nodeDefinition(nodeKey, WorkflowNodeType.APPROVAL, participantPolicyText);
    }

    private WorkflowNodeDefinition nodeDefinition(String nodeKey, WorkflowNodeType nodeType,
                                                  String participantPolicyText) {
        WorkflowNodeDefinition definition = new WorkflowNodeDefinition();
        definition.setNodeKey(nodeKey);
        definition.setNodeType(nodeType);
        definition.setApprovalMode(WorkflowApprovalMode.ALL);
        definition.setParticipantPolicyText(participantPolicyText);
        definition.setAllowReject(true);
        definition.setAllowRollback(true);
        definition.setAllowAddSign(true);
        definition.setWarningDurationMinutes(30);
        definition.setOvertimeDurationMinutes(60);
        definition.setNodeConfigText("{\"source\":\"test\"}");
        return definition;
    }

    private WorkflowLinkDefinition linkDefinition(String routeKey, String sourceNodeKey, String targetNodeKey) {
        WorkflowLinkDefinition definition = new WorkflowLinkDefinition();
        definition.setRouteKey(routeKey);
        definition.setSourceNodeKey(sourceNodeKey);
        definition.setTargetNodeKey(targetNodeKey);
        definition.setDefaultRoute(false);
        return definition;
    }

    private WorkflowNodeInstance node(WorkflowApprovalMode mode, Integer ratio) {
        WorkflowNodeInstance node = node("node-1", "approve", WorkflowNodeType.APPROVAL, WorkflowNodeStatus.ACTIVE);
        node.setApprovalMode(mode);
        node.setApprovalRatio(ratio);
        return node;
    }

    private WorkflowNodeInstance node(String id, String nodeKey, WorkflowNodeType nodeType, WorkflowNodeStatus status) {
        WorkflowNodeInstance node = new WorkflowNodeInstance();
        node.setId(id);
        node.setTenantId("tenant-1");
        node.setVersion(2);
        node.setInstanceId("instance-1");
        node.setNodeKey(nodeKey);
        node.setNodeRunId(nodeKey + ":1");
        node.setNodeType(nodeType);
        node.setNodeStatus(status);
        return node;
    }

    private WorkflowRouteInstance route(String id, String source, String target, WorkflowRouteStatus status) {
        WorkflowRouteInstance route = new WorkflowRouteInstance();
        route.setId(id);
        route.setTenantId("tenant-1");
        route.setVersion(4);
        route.setInstanceId("instance-1");
        route.setRouteKey(source + "-" + target);
        route.setRouteRunId(source + "-" + target + ":1");
        route.setSourceNodeKey(source);
        route.setTargetNodeKey(target);
        route.setRouteStatus(status);
        return route;
    }

    private WorkflowInstance instance() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId("instance-1");
        instance.setTenantId("tenant-1");
        instance.setVersion(5);
        instance.setModuleAlias("sales.contract");
        instance.setRecordId("record-1");
        instance.setInstanceStatus(WorkflowInstanceStatus.RUNNING);
        instance.setApprovalStatus(WorkflowApprovalStatus.PROCESSING);
        return instance;
    }
}
