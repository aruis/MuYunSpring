package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WorkflowAdminServiceTest {
    private final WorkflowInstanceDao instanceDao = mock(WorkflowInstanceDao.class);
    private final WorkflowTaskDao taskDao = mock(WorkflowTaskDao.class);
    private final WorkflowNodeInstanceDao nodeInstanceDao = mock(WorkflowNodeInstanceDao.class);
    private final WorkflowActionPolicyService actionPolicyService = mock(WorkflowActionPolicyService.class);
    private final WorkflowInstanceActionService instanceActionService = mock(WorkflowInstanceActionService.class);
    private final WorkflowTaskActionService taskActionService = mock(WorkflowTaskActionService.class);
    private final WorkflowHistoryQueryService historyQueryService = mock(WorkflowHistoryQueryService.class);
    private final WorkflowAdminService service = new WorkflowAdminService(instanceDao, taskDao, nodeInstanceDao,
            actionPolicyService, instanceActionService, taskActionService, historyQueryService);

    @Test
    void shouldQueryCurrentTodoTasksThroughForceApprovePolicy() {
        WorkflowInstance instance = instance(WorkflowInstanceStatus.RUNNING);
        WorkflowTask task = new WorkflowTask();
        task.setId("task-1");
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(taskDao.query(any(), any(), any())).thenReturn(List.of(task));

        List<WorkflowTask> tasks = service.currentTodoTasks("instance-1");

        assertThat(tasks).containsExactly(task);
        verify(actionPolicyService).requireManagementAction(
                WorkflowActionPolicyService.MANAGEMENT_TODO_TASK_QUERY_ACTION);
    }

    @Test
    void shouldExposeActiveApprovalTodoTaskViewsOnly() {
        WorkflowInstance instance = instance(WorkflowInstanceStatus.RUNNING);
        WorkflowTask activeApproval = task("task-1", "node-active", WorkflowTaskKind.APPROVAL,
                WorkflowTaskStatus.TODO);
        activeApproval.setAssigneeId("delegate-1");
        activeApproval.setAssignmentKind(WorkflowAssignmentKind.DELEGATED);
        activeApproval.setOriginalAssigneeId("principal-1");
        activeApproval.setDelegatedFromUserId("principal-1");
        activeApproval.setDelegatedToUserId("delegate-1");
        activeApproval.setPrincipalCanProcess(true);
        activeApproval.setDelegationPolicyId("delegation-1");
        activeApproval.setAssignmentSnapshotText("{\"delegationPolicyId\":\"delegation-1\"}");
        activeApproval.setCreatedAt(Instant.parse("2026-06-05T01:00:00Z"));
        WorkflowTask notice = task("task-notice", "node-active", WorkflowTaskKind.NOTICE, WorkflowTaskStatus.TODO);
        WorkflowTask business = task("task-business", "node-active", WorkflowTaskKind.BUSINESS,
                WorkflowTaskStatus.TODO);
        WorkflowTask done = task("task-done", "node-active", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.DONE);
        WorkflowTask inactiveNodeTask = task("task-inactive", "node-inactive", WorkflowTaskKind.APPROVAL,
                WorkflowTaskStatus.TODO);
        WorkflowTask taskNodeTask = task("task-node", "node-task", WorkflowTaskKind.APPROVAL,
                WorkflowTaskStatus.TODO);
        WorkflowNodeInstance activeNode = node("node-active", WorkflowNodeStatus.ACTIVE, WorkflowNodeType.APPROVAL);
        activeNode.setOvertimeStatus(WorkflowOvertimeStatus.WARNED);
        WorkflowNodeInstance inactiveNode = node("node-inactive", WorkflowNodeStatus.COMPLETED,
                WorkflowNodeType.APPROVAL);
        WorkflowNodeInstance taskNode = node("node-task", WorkflowNodeStatus.ACTIVE, WorkflowNodeType.TASK);
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(taskDao.query(any(), any(), any())).thenReturn(List.of(
                activeApproval, notice, business, done, inactiveNodeTask, taskNodeTask));
        when(nodeInstanceDao.findById("node-active")).thenReturn(activeNode);
        when(nodeInstanceDao.findById("node-inactive")).thenReturn(inactiveNode);
        when(nodeInstanceDao.findById("node-task")).thenReturn(taskNode);

        List<WorkflowAdminActiveTaskView> views = service.currentTodoTaskViews("instance-1");

        assertThat(views).hasSize(1);
        WorkflowAdminActiveTaskView view = views.getFirst();
        assertThat(view.taskId()).isEqualTo("task-1");
        assertThat(view.instanceId()).isEqualTo("instance-1");
        assertThat(view.nodeInstanceId()).isEqualTo("node-active");
        assertThat(view.nodeKey()).isEqualTo("approve_1");
        assertThat(view.taskKind()).isEqualTo(WorkflowTaskKind.APPROVAL);
        assertThat(view.taskStatus()).isEqualTo(WorkflowTaskStatus.TODO);
        assertThat(view.assigneeId()).isEqualTo("delegate-1");
        assertThat(view.createdAt()).isEqualTo(Instant.parse("2026-06-05T01:00:00Z"));
        assertThat(view.receivedAt()).isEqualTo(Instant.parse("2026-06-05T01:00:00Z"));
        assertThat(view.overtimeStatus()).isEqualTo(WorkflowOvertimeStatus.WARNED);
        assertThat(view.canForceApprove()).isTrue();
        assertThat(view.assignmentKind()).isEqualTo(WorkflowAssignmentKind.DELEGATED);
        assertThat(view.originalAssigneeId()).isEqualTo("principal-1");
        assertThat(view.delegatedFromUserId()).isEqualTo("principal-1");
        assertThat(view.delegatedToUserId()).isEqualTo("delegate-1");
        assertThat(view.principalCanProcess()).isTrue();
        assertThat(view.delegationPolicyId()).isEqualTo("delegation-1");
        assertThat(view.delegationSnapshot()).contains("delegation-1");
        verify(actionPolicyService).requireManagementAction(
                WorkflowActionPolicyService.MANAGEMENT_TODO_TASK_QUERY_ACTION);
    }

    @Test
    void shouldRejectTodoQueryForNonRunningInstance() {
        when(instanceDao.findById("instance-1")).thenReturn(instance(WorkflowInstanceStatus.COMPLETED));

        assertThatThrownBy(() -> service.currentTodoTasks("instance-1"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("not running");

        verifyNoInteractions(taskDao, nodeInstanceDao, actionPolicyService);
    }

    @Test
    void shouldDelegateManagementActions() {
        WorkflowInstanceActionRequest instanceRequest = WorkflowInstanceActionRequest.terminate(
                "instance-1", "admin-1", "force stop");
        WorkflowTaskActionRequest taskRequest = WorkflowTaskActionRequest.complete(
                "task-1", "admin-1", "force approve");

        service.forceTerminate(instanceRequest);
        service.forceApprove(taskRequest);

        verify(instanceActionService).forceTerminate(instanceRequest);
        verify(taskActionService).forceApprove(taskRequest);
    }

    @Test
    void shouldDelegateAdminHistoryContracts() {
        WorkflowRuntimeRenderBundle bundle = new WorkflowRuntimeRenderBundle("HISTORY", null, List.of(), List.of());
        PageRequest pageRequest = PageRequest.of(1, 20);
        when(historyQueryService.queryAdminHistory("sales.contract", "record-1", pageRequest)).thenReturn(List.of());
        when(historyQueryService.renderAdminBundle("history-1")).thenReturn(bundle);
        when(historyQueryService.adminEvents("history-1")).thenReturn(List.of());
        when(historyQueryService.adminEventViews("history-1")).thenReturn(List.of());
        when(historyQueryService.deleteHistory("history-1")).thenReturn(1);

        assertThat(service.queryHistory("sales.contract", "record-1", pageRequest)).isEmpty();
        assertThat(service.renderHistoryBundle("history-1")).isEqualTo(bundle);
        assertThat(service.historyEvents("history-1")).isEmpty();
        assertThat(service.historyEventViews("history-1")).isEmpty();
        assertThat(service.deleteHistory("history-1")).isEqualTo(1);

        verify(historyQueryService).queryAdminHistory("sales.contract", "record-1", pageRequest);
        verify(historyQueryService).renderAdminBundle("history-1");
        verify(historyQueryService).adminEvents("history-1");
        verify(historyQueryService).adminEventViews("history-1");
        verify(historyQueryService).deleteHistory("history-1");
    }

    private WorkflowInstance instance(WorkflowInstanceStatus status) {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId("instance-1");
        instance.setModuleAlias("sales.contract");
        instance.setRecordId("record-1");
        instance.setInstanceStatus(status);
        return instance;
    }

    private WorkflowTask task(String id, String nodeInstanceId, WorkflowTaskKind kind, WorkflowTaskStatus status) {
        WorkflowTask task = new WorkflowTask();
        task.setId(id);
        task.setInstanceId("instance-1");
        task.setNodeInstanceId(nodeInstanceId);
        task.setTaskKind(kind);
        task.setTaskStatus(status);
        return task;
    }

    private WorkflowNodeInstance node(String id, WorkflowNodeStatus status, WorkflowNodeType type) {
        WorkflowNodeInstance node = new WorkflowNodeInstance();
        node.setId(id);
        node.setInstanceId("instance-1");
        node.setNodeKey("approve_1");
        node.setNodeStatus(status);
        node.setNodeType(type);
        return node;
    }
}
