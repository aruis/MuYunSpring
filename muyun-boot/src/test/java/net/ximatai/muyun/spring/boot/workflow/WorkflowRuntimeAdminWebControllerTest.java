package net.ximatai.muyun.spring.boot.workflow;

import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.workflow.WorkflowAdminFacade;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstance;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceActionRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceActionResult;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTask;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskActionRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskActionResult;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskKind;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkflowRuntimeAdminWebControllerTest {
    private WorkflowAdminFacade adminFacade;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        adminFacade = mock(WorkflowAdminFacade.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new WorkflowRuntimeAdminWebController(adminFacade))
                .addFilters(new CurrentUserWebFilter(() -> Optional.of(
                        CurrentUser.tenantUser("user-1", "User", "tenant-a"))))
                .build();
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        TenantContext.clear();
    }

    @Test
    void shouldExposeCurrentTodoTasks() throws Exception {
        WorkflowTask task = new WorkflowTask();
        task.setId("task-1");
        task.setTaskKind(WorkflowTaskKind.APPROVAL);
        task.setTaskStatus(WorkflowTaskStatus.TODO);
        when(adminFacade.currentTodoTasks("inst-1")).thenReturn(List.of(task));

        mvc.perform(get("/workflow/runtime/admin/instance/inst-1/todo-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("task-1"));
    }

    @Test
    void shouldExecuteManagementActionsWithCurrentUserFallback() throws Exception {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId("inst-1");
        WorkflowTask task = new WorkflowTask();
        task.setId("task-1");
        when(adminFacade.forceTerminate(argThat(request ->
                "inst-1".equals(request.instanceId())
                        && "admin-1".equals(request.operatorId())
                        && "force stop".equals(request.reason()))))
                .thenReturn(new WorkflowInstanceActionResult(instance, List.of(), List.of(), List.of(), null));
        when(adminFacade.forceApprove(argThat(request ->
                "task-1".equals(request.taskId())
                        && "user-1".equals(request.operatorId())
                        && "force agree".equals(request.reason()))))
                .thenReturn(WorkflowTaskActionResult.of(task, null));

        mvc.perform(post("/workflow/runtime/admin/instance/inst-1/actions/forceTerminate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operatorId\":\"admin-1\",\"reason\":\"force stop\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instance.id").value("inst-1"));

        mvc.perform(post("/workflow/runtime/admin/task/task-1/actions/forceApprove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"force agree\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.task.id").value("task-1"));
    }
}
