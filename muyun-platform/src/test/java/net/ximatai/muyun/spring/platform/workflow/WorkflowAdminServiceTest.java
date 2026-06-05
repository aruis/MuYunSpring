package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.junit.jupiter.api.Test;

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
    private final WorkflowActionPolicyService actionPolicyService = mock(WorkflowActionPolicyService.class);
    private final WorkflowInstanceActionService instanceActionService = mock(WorkflowInstanceActionService.class);
    private final WorkflowTaskActionService taskActionService = mock(WorkflowTaskActionService.class);
    private final WorkflowHistoryQueryService historyQueryService = mock(WorkflowHistoryQueryService.class);
    private final WorkflowAdminService service = new WorkflowAdminService(instanceDao, taskDao, actionPolicyService,
            instanceActionService, taskActionService, historyQueryService);

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
    void shouldRejectTodoQueryForNonRunningInstance() {
        when(instanceDao.findById("instance-1")).thenReturn(instance(WorkflowInstanceStatus.COMPLETED));

        assertThatThrownBy(() -> service.currentTodoTasks("instance-1"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("not running");

        verifyNoInteractions(taskDao, actionPolicyService);
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
}
