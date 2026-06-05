package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.PageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowAdminFacadeTest {
    private final WorkflowAdminService adminService = mock(WorkflowAdminService.class);
    private final WorkflowAdminFacade facade = new WorkflowAdminFacade(adminService);

    @Test
    void shouldDelegateAdminContracts() {
        WorkflowTask task = new WorkflowTask();
        task.setId("task-1");
        WorkflowAdminActiveTaskView view = new WorkflowAdminActiveTaskView("task-1", "instance-1", "node-1",
                "approve_1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO, "approver-1",
                null, null, WorkflowOvertimeStatus.NORMAL, true, WorkflowAssignmentKind.NORMAL,
                "approver-1", null, null, null, null, null);
        when(adminService.currentTodoTasks("instance-1")).thenReturn(List.of(task));
        when(adminService.currentTodoTaskViews("instance-1")).thenReturn(List.of(view));
        WorkflowInstanceActionRequest instanceRequest = WorkflowInstanceActionRequest.terminate(
                "instance-1", "admin-1", "force stop");
        WorkflowTaskActionRequest taskRequest = WorkflowTaskActionRequest.complete(
                "task-1", "admin-1", "force approve");
        WorkflowRuntimeRenderBundle bundle = new WorkflowRuntimeRenderBundle("HISTORY", null, List.of(), List.of());
        PageRequest pageRequest = PageRequest.of(1, 20);
        when(adminService.queryHistory("sales.contract", "record-1", pageRequest)).thenReturn(List.of());
        when(adminService.renderHistoryBundle("history-1")).thenReturn(bundle);
        when(adminService.historyEvents("history-1")).thenReturn(List.of());
        when(adminService.historyEventViews("history-1")).thenReturn(List.of());
        when(adminService.deleteHistory("history-1")).thenReturn(1);

        assertThat(facade.currentTodoTasks("instance-1")).containsExactly(task);
        assertThat(facade.currentTodoTaskViews("instance-1")).containsExactly(view);
        facade.forceTerminate(instanceRequest);
        facade.forceApprove(taskRequest);
        assertThat(facade.queryHistory("sales.contract", "record-1", pageRequest)).isEmpty();
        assertThat(facade.renderHistoryBundle("history-1")).isEqualTo(bundle);
        assertThat(facade.historyEvents("history-1")).isEmpty();
        assertThat(facade.historyEventViews("history-1")).isEmpty();
        assertThat(facade.deleteHistory("history-1")).isEqualTo(1);

        verify(adminService).forceTerminate(instanceRequest);
        verify(adminService).forceApprove(taskRequest);
        verify(adminService).queryHistory("sales.contract", "record-1", pageRequest);
        verify(adminService).renderHistoryBundle("history-1");
        verify(adminService).historyEvents("history-1");
        verify(adminService).historyEventViews("history-1");
        verify(adminService).deleteHistory("history-1");
    }
}
