package net.ximatai.muyun.spring.platform.workflow;

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
        when(adminService.currentTodoTasks("instance-1")).thenReturn(List.of(task));
        WorkflowInstanceActionRequest instanceRequest = WorkflowInstanceActionRequest.terminate(
                "instance-1", "admin-1", "force stop");
        WorkflowTaskActionRequest taskRequest = WorkflowTaskActionRequest.complete(
                "task-1", "admin-1", "force approve");

        assertThat(facade.currentTodoTasks("instance-1")).containsExactly(task);
        facade.forceTerminate(instanceRequest);
        facade.forceApprove(taskRequest);

        verify(adminService).forceTerminate(instanceRequest);
        verify(adminService).forceApprove(taskRequest);
    }
}
