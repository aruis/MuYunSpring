package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WorkflowInstanceActionServiceTest {
    private final WorkflowInstanceDao instanceDao = mock(WorkflowInstanceDao.class);
    private final WorkflowNodeInstanceDao nodeDao = mock(WorkflowNodeInstanceDao.class);
    private final WorkflowRouteInstanceDao routeDao = mock(WorkflowRouteInstanceDao.class);
    private final WorkflowTaskDao taskDao = mock(WorkflowTaskDao.class);
    private final WorkflowEventDao eventDao = mock(WorkflowEventDao.class);
    private final WorkflowRuntimeEventFactory eventFactory = new WorkflowRuntimeEventFactory();
    private final WorkflowArchiveService archiveService = mock(WorkflowArchiveService.class);
    private final WorkflowApprovalSummaryWriter summaryWriter = mock(WorkflowApprovalSummaryWriter.class);
    private final WorkflowInstanceActionService service = new WorkflowInstanceActionService(
            instanceDao, nodeDao, routeDao, taskDao, eventDao, eventFactory, archiveService, Optional.of(summaryWriter));

    @Test
    void shouldRevokeRunningInstanceAndCancelOpenRuntimeObjects() {
        WorkflowInstance instance = instance(true);
        WorkflowTask task = task("task-1");
        WorkflowNodeInstance node = node("node-1");
        WorkflowRouteInstance route = route("route-1", WorkflowRouteStatus.EFFECTIVE);
        stubRuntime(instance, List.of(task), List.of(node), List.of(route));

        WorkflowInstanceActionResult result = service.revoke(new WorkflowInstanceActionRequest(
                "instance-1", "user-1", "withdraw", Instant.parse("2026-06-05T04:00:00Z")));

        assertThat(result.instance().getInstanceStatus()).isEqualTo(WorkflowInstanceStatus.REVOKED);
        assertThat(result.instance().getApprovalStatus()).isEqualTo(WorkflowApprovalStatus.REVOKED);
        assertThat(task.getTaskStatus()).isEqualTo(WorkflowTaskStatus.CANCELED);
        assertThat(node.getNodeStatus()).isEqualTo(WorkflowNodeStatus.CANCELED);
        assertThat(route.getRouteStatus()).isEqualTo(WorkflowRouteStatus.CANCELED);
        assertThat(result.event().getEventType()).isEqualTo(WorkflowEventType.INSTANCE_REVOKED);
        verify(instanceDao).updateByIdAndVersion(instance, 5);
        verify(taskDao).updateByIdAndVersion(task, 3);
        verify(nodeDao).updateByIdAndVersion(node, 2);
        verify(routeDao).updateByIdAndVersion(route, 4);
        verify(eventDao).insert(result.event());
        verify(archiveService).archiveCurrentInstance(instance, WorkflowArchiveReason.RECALLED,
                Instant.parse("2026-06-05T04:00:00Z"));
        verify(summaryWriter).clearCurrent("sales.contract", "record-1");
        verify(summaryWriter, never()).writeSubmitted(any());
    }

    @Test
    void shouldTerminateRunningInstanceAndInvalidateOpenTasks() {
        WorkflowInstance instance = instance(true);
        WorkflowTask task = task("task-1");
        WorkflowNodeInstance node = node("node-1");
        WorkflowRouteInstance route = route("route-1", WorkflowRouteStatus.CANDIDATE);
        stubRuntime(instance, List.of(task), List.of(node), List.of(route));

        WorkflowInstanceActionResult result = service.terminate(new WorkflowInstanceActionRequest(
                "instance-1", "admin-1", "stop", Instant.parse("2026-06-05T04:00:00Z")));

        assertThat(result.instance().getInstanceStatus()).isEqualTo(WorkflowInstanceStatus.TERMINATED);
        assertThat(result.instance().getApprovalStatus()).isEqualTo(WorkflowApprovalStatus.TERMINATED);
        assertThat(result.instance().getTerminatedAt()).isEqualTo(Instant.parse("2026-06-05T04:00:00Z"));
        assertThat(task.getTaskStatus()).isEqualTo(WorkflowTaskStatus.INVALIDATED);
        assertThat(node.getNodeStatus()).isEqualTo(WorkflowNodeStatus.CANCELED);
        assertThat(route.getRouteStatus()).isEqualTo(WorkflowRouteStatus.CANCELED);
        assertThat(result.event().getEventType()).isEqualTo(WorkflowEventType.INSTANCE_TERMINATED);
    }

    @Test
    void shouldForceTerminateWithManagementActionCodeAndWithoutArchive() {
        WorkflowInstance instance = instance(true);
        WorkflowTask task = task("task-1");
        WorkflowNodeInstance node = node("node-1");
        WorkflowRouteInstance route = route("route-1", WorkflowRouteStatus.EFFECTIVE);
        stubRuntime(instance, List.of(task), List.of(node), List.of(route));

        WorkflowInstanceActionResult result = service.forceTerminate(new WorkflowInstanceActionRequest(
                "instance-1", "admin-1", "force stop", Instant.parse("2026-06-05T04:30:00Z")));

        assertThat(result.instance().getInstanceStatus()).isEqualTo(WorkflowInstanceStatus.TERMINATED);
        assertThat(result.instance().getApprovalStatus()).isEqualTo(WorkflowApprovalStatus.TERMINATED);
        assertThat(result.instance().getLastActionCode()).isEqualTo("forceTerminate");
        assertThat(task.getTaskStatus()).isEqualTo(WorkflowTaskStatus.INVALIDATED);
        assertThat(node.getNodeStatus()).isEqualTo(WorkflowNodeStatus.CANCELED);
        assertThat(route.getRouteStatus()).isEqualTo(WorkflowRouteStatus.CANCELED);
        assertThat(result.event().getEventType()).isEqualTo(WorkflowEventType.INSTANCE_TERMINATED);
        assertThat(result.event().getActionCode()).isEqualTo("forceTerminate");
        verify(archiveService, never()).archiveCurrentInstance(any(), any(), any());
        verify(summaryWriter).writeSubmitted(any());
    }

    @Test
    void shouldResetInstanceAndArchiveCurrentRuntimeObjects() {
        WorkflowInstance instance = instance(true);
        WorkflowTask task = task("task-1");
        WorkflowNodeInstance node = node("node-1");
        WorkflowRouteInstance route = route("route-1", WorkflowRouteStatus.CANDIDATE);
        stubRuntime(instance, List.of(task), List.of(node), List.of(route));

        WorkflowInstanceActionResult result = service.reset(new WorkflowInstanceActionRequest(
                "instance-1", "admin-1", "reset", Instant.parse("2026-06-05T05:00:00Z")));

        assertThat(result.instance().getInstanceStatus()).isEqualTo(WorkflowInstanceStatus.RUNNING);
        assertThat(result.instance().getLastActionCode()).isEqualTo("reset");
        assertThat(task.getTaskStatus()).isEqualTo(WorkflowTaskStatus.CANCELED);
        assertThat(node.getNodeStatus()).isEqualTo(WorkflowNodeStatus.CANCELED);
        assertThat(route.getRouteStatus()).isEqualTo(WorkflowRouteStatus.CANCELED);
        assertThat(result.event().getEventType()).isEqualTo(WorkflowEventType.INSTANCE_RESET);
        verify(archiveService).archiveCurrentInstance(instance, WorkflowArchiveReason.RESET,
                Instant.parse("2026-06-05T05:00:00Z"));
        verify(summaryWriter).clearCurrent("sales.contract", "record-1");
    }

    @Test
    void shouldRejectClosingNonRunningInstance() {
        WorkflowInstance instance = instance(false);
        instance.setInstanceStatus(WorkflowInstanceStatus.COMPLETED);
        when(instanceDao.findById("instance-1")).thenReturn(instance);

        assertThatThrownBy(() -> service.revoke(WorkflowInstanceActionRequest.revoke(
                "instance-1", "user-1", "late")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("not running");

        verifyNoInteractions(taskDao, nodeDao, routeDao, eventDao);
    }

    @Test
    void shouldRequireReasonForCloseInstanceAction() {
        WorkflowInstance instance = instance(true);
        when(instanceDao.findById("instance-1")).thenReturn(instance);

        assertThatThrownBy(() -> service.terminate(WorkflowInstanceActionRequest.terminate(
                "instance-1", "admin-1", null)))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("reason is required");

        verifyNoInteractions(taskDao, nodeDao, routeDao, eventDao);
    }

    @Test
    void shouldRequireReasonForForceTerminate() {
        WorkflowInstance instance = instance(true);
        when(instanceDao.findById("instance-1")).thenReturn(instance);

        assertThatThrownBy(() -> service.forceTerminate(new WorkflowInstanceActionRequest(
                "instance-1", "admin-1", null, null)))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("reason is required");

        verifyNoInteractions(taskDao, nodeDao, routeDao, eventDao);
    }

    private void stubRuntime(WorkflowInstance instance, List<WorkflowTask> tasks,
                             List<WorkflowNodeInstance> nodes, List<WorkflowRouteInstance> routes) {
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(taskDao.query(any(), any())).thenReturn(tasks);
        when(nodeDao.query(any(), any())).thenReturn(nodes);
        when(routeDao.query(any(), any())).thenReturn(routes);
        when(instanceDao.updateByIdAndVersion(instance, 5)).thenReturn(1);
        tasks.forEach(task -> when(taskDao.updateByIdAndVersion(task, 3)).thenReturn(1));
        nodes.forEach(node -> when(nodeDao.updateByIdAndVersion(node, 2)).thenReturn(1));
        routes.forEach(route -> when(routeDao.updateByIdAndVersion(route, 4)).thenReturn(1));
    }

    private WorkflowInstance instance(boolean approvalEnabled) {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId("instance-1");
        instance.setTenantId("tenant-1");
        instance.setVersion(5);
        instance.setModuleAlias("sales.contract");
        instance.setRecordId("record-1");
        instance.setInstanceStatus(WorkflowInstanceStatus.RUNNING);
        instance.setApprovalEnabled(approvalEnabled);
        instance.setApprovalStatus(approvalEnabled ? WorkflowApprovalStatus.PROCESSING : WorkflowApprovalStatus.NONE);
        instance.setStartedBy("starter-1");
        instance.setStartedAt(Instant.parse("2026-06-05T01:00:00Z"));
        instance.setSnapshotText("{}");
        return instance;
    }

    private WorkflowTask task(String id) {
        WorkflowTask task = new WorkflowTask();
        task.setId(id);
        task.setTenantId("tenant-1");
        task.setVersion(3);
        task.setInstanceId("instance-1");
        task.setTaskStatus(WorkflowTaskStatus.TODO);
        task.setTaskKind(WorkflowTaskKind.APPROVAL);
        return task;
    }

    private WorkflowNodeInstance node(String id) {
        WorkflowNodeInstance node = new WorkflowNodeInstance();
        node.setId(id);
        node.setTenantId("tenant-1");
        node.setVersion(2);
        node.setInstanceId("instance-1");
        node.setNodeStatus(WorkflowNodeStatus.ACTIVE);
        node.setNodeKey("approve");
        node.setNodeRunId("approve:1");
        node.setNodeType(WorkflowNodeType.APPROVAL);
        return node;
    }

    private WorkflowRouteInstance route(String id, WorkflowRouteStatus status) {
        WorkflowRouteInstance route = new WorkflowRouteInstance();
        route.setId(id);
        route.setTenantId("tenant-1");
        route.setVersion(4);
        route.setInstanceId("instance-1");
        route.setRouteStatus(status);
        route.setRouteKey("route-1");
        route.setRouteRunId("route-1:1");
        route.setSourceNodeKey("start");
        route.setTargetNodeKey("approve");
        return route;
    }
}
