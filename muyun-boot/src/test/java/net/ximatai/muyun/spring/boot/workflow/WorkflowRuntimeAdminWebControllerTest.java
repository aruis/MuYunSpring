package net.ximatai.muyun.spring.boot.workflow;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.workflow.WorkflowActionPolicyService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowAdminActiveTaskView;
import net.ximatai.muyun.spring.platform.workflow.WorkflowAdminFacade;
import net.ximatai.muyun.spring.platform.workflow.WorkflowAdminInstanceQueryRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowAdminInstanceView;
import net.ximatai.muyun.spring.platform.workflow.WorkflowApprovalStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowEvent;
import net.ximatai.muyun.spring.platform.workflow.WorkflowEventType;
import net.ximatai.muyun.spring.platform.workflow.WorkflowHistoryEventView;
import net.ximatai.muyun.spring.platform.workflow.WorkflowHistoryInstance;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstance;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceActionRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceActionResult;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowManualRouteSelection;
import net.ximatai.muyun.spring.platform.workflow.WorkflowOvertimeStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRuntimeRenderBundle;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTask;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskActionRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskActionResult;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskKind;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowRuntimeAdminWebControllerTest {
    private WorkflowAdminFacade adminFacade;
    private WorkflowRuntimeAdminWebController controller;

    @BeforeEach
    void setUp() {
        adminFacade = mock(WorkflowAdminFacade.class);
        controller = new WorkflowRuntimeAdminWebController(adminFacade);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        TenantContext.clear();
    }

    @Test
    void shouldDeclareWorkflowRuntimeAdminRoutesAndPlatformMetadata() throws Exception {
        assertThat(WorkflowRuntimeAdminWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/workflow/runtime/admin");
        PlatformStaticModule module = WorkflowRuntimeAdminWebController.class.getAnnotation(PlatformStaticModule.class);
        assertThat(module.application()).isEqualTo("platform");
        assertThat(module.alias()).isEqualTo(WorkflowActionPolicyService.MANAGEMENT_MODULE_ALIAS);
        PlatformMenu menu = WorkflowRuntimeAdminWebController.class.getAnnotation(PlatformMenu.class);
        assertThat(menu.parent()).isEqualTo(PlatformMenuGroups.OPS);

        assertRoute("currentTodoTasks", new Class<?>[]{String.class},
                GET.class, "/instance/{instanceId}/todo-tasks",
                WorkflowActionPolicyService.MANAGEMENT_TODO_TASK_QUERY_ACTION);
        assertRoute("queryCurrentInstances", new Class<?>[]{WorkflowAdminInstanceQueryWebRequest.class},
                POST.class, "/instance/query", WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION);
        assertRoute("forceTerminate", new Class<?>[]{String.class, WorkflowAdminActionWebRequest.class},
                POST.class, "/instance/{instanceId}/actions/forceTerminate",
                WorkflowActionPolicyService.MANAGEMENT_FORCE_TERMINATE_ACTION);
        assertRoute("reset", new Class<?>[]{String.class, WorkflowAdminActionWebRequest.class},
                POST.class, "/instance/{instanceId}/actions/reset",
                WorkflowActionPolicyService.MANAGEMENT_RESET_ACTION);
        assertRoute("forceApprove", new Class<?>[]{String.class, WorkflowAdminTaskActionWebRequest.class},
                POST.class, "/task/{taskId}/actions/forceApprove",
                WorkflowActionPolicyService.MANAGEMENT_FORCE_APPROVE_ACTION);
        assertRoute("deleteHistory", new Class<?>[]{String.class, Object.class},
                POST.class, "/history/{historyInstanceId}/delete",
                WorkflowActionPolicyService.MANAGEMENT_DELETE_HISTORY_ACTION);
    }

    @Test
    void shouldExposeCurrentInstanceQueriesAndDetails() {
        WorkflowTask task = new WorkflowTask();
        task.setId("task-1");
        task.setTaskKind(WorkflowTaskKind.APPROVAL);
        task.setTaskStatus(WorkflowTaskStatus.TODO);
        WorkflowAdminActiveTaskView activeTask = activeTaskView();
        WorkflowAdminInstanceView instanceView = instanceView();
        WorkflowEvent event = event("event-1", WorkflowEventType.TASK_COMPLETED);
        when(adminFacade.currentTodoTasks("inst-1")).thenReturn(List.of(task));
        when(adminFacade.currentTodoTaskViews("inst-1")).thenReturn(List.of(activeTask));
        when(adminFacade.queryCurrentInstances(any(), any())).thenReturn(List.of(instanceView));
        when(adminFacade.renderCurrentBundle("inst-1"))
                .thenReturn(new WorkflowRuntimeRenderBundle("RUNTIME", null, List.of(), List.of()));
        when(adminFacade.currentEvents("inst-1")).thenReturn(List.of(event));
        when(adminFacade.currentTasks("inst-1")).thenReturn(List.of(task));

        WebListResponse<WorkflowTask> todoTasks = controller.currentTodoTasks("inst-1");
        WebListResponse<WorkflowAdminActiveTaskView> activeTasks = controller.currentTodoTaskViews("inst-1");
        WebListResponse<WorkflowAdminInstanceView> instances = controller.queryCurrentInstances(
                new WorkflowAdminInstanceQueryWebRequest(
                        "sales.contract",
                        "record-1",
                        "starter-1",
                        WorkflowInstanceStatus.RUNNING,
                        WorkflowApprovalStatus.PROCESSING,
                        "approver-1",
                        WorkflowOvertimeStatus.WARNED,
                        new WebPageRequest(2, 30)
                ));

        assertThat(todoTasks.records()).singleElement().extracting(WorkflowTask::getId).isEqualTo("task-1");
        assertThat(activeTasks.records()).singleElement()
                .extracting(WorkflowAdminActiveTaskView::taskId)
                .isEqualTo("task-1");
        assertThat(instances.records()).singleElement()
                .extracting(WorkflowAdminInstanceView::instanceId)
                .isEqualTo("inst-1");
        assertThat(controller.renderCurrentBundle("inst-1", null).mode()).isEqualTo("RUNTIME");
        assertThat(controller.currentEvents("inst-1", null).records()).singleElement()
                .extracting(WorkflowEvent::getId)
                .isEqualTo("event-1");
        assertThat(controller.currentTasks("inst-1", null).records()).singleElement()
                .extracting(WorkflowTask::getId)
                .isEqualTo("task-1");
        ArgumentCaptor<WorkflowAdminInstanceQueryRequest> query =
                ArgumentCaptor.forClass(WorkflowAdminInstanceQueryRequest.class);
        ArgumentCaptor<PageRequest> page = ArgumentCaptor.forClass(PageRequest.class);
        verify(adminFacade).queryCurrentInstances(query.capture(), page.capture());
        assertThat(query.getValue().moduleAlias()).isEqualTo("sales.contract");
        assertThat(query.getValue().recordId()).isEqualTo("record-1");
        assertThat(query.getValue().starterId()).isEqualTo("starter-1");
        assertThat(query.getValue().approvalStatus()).isEqualTo(WorkflowApprovalStatus.PROCESSING);
        assertThat(query.getValue().currentAssigneeId()).isEqualTo("approver-1");
        assertThat(page.getValue().getOffset()).isEqualTo(30);
        assertThat(page.getValue().getLimit()).isEqualTo(30);
    }

    @Test
    void shouldExecuteManagementActionsWithExplicitAndCurrentOperator() {
        WorkflowInstanceActionResult instanceResult = new WorkflowInstanceActionResult(
                instance("inst-1"), List.of(), List.of(), List.of(), null);
        WorkflowTaskActionResult taskResult = WorkflowTaskActionResult.of(task("task-1"), null);
        when(adminFacade.forceTerminate(any())).thenReturn(instanceResult);
        when(adminFacade.reset(any())).thenReturn(instanceResult);
        when(adminFacade.forceApprove(any())).thenReturn(taskResult);

        WorkflowInstanceActionResult forceTerminateResult;
        WorkflowInstanceActionResult resetResult;
        WorkflowTaskActionResult forceApproveResult;
        try (CurrentUserContext.Scope ignored = currentUser()) {
            forceTerminateResult = controller.forceTerminate("inst-1",
                    new WorkflowAdminActionWebRequest("admin-1", "force stop"));
            resetResult = controller.reset("inst-1",
                    new WorkflowAdminActionWebRequest(null, "reset workflow"));
            forceApproveResult = controller.forceApprove("task-1",
                    new WorkflowAdminTaskActionWebRequest(
                            null,
                            "force agree",
                            null,
                            "leftRoute",
                            "choose left",
                            List.of(new WorkflowManualRouteSelection("branchA", "routeA1", "choose A1"))
                    ));
        }

        assertThat(forceTerminateResult).isSameAs(instanceResult);
        assertThat(resetResult).isSameAs(instanceResult);
        assertThat(forceApproveResult).isSameAs(taskResult);
        ArgumentCaptor<WorkflowInstanceActionRequest> terminate =
                ArgumentCaptor.forClass(WorkflowInstanceActionRequest.class);
        ArgumentCaptor<WorkflowInstanceActionRequest> reset =
                ArgumentCaptor.forClass(WorkflowInstanceActionRequest.class);
        ArgumentCaptor<WorkflowTaskActionRequest> approve =
                ArgumentCaptor.forClass(WorkflowTaskActionRequest.class);
        verify(adminFacade).forceTerminate(terminate.capture());
        verify(adminFacade).reset(reset.capture());
        verify(adminFacade).forceApprove(approve.capture());
        assertThat(terminate.getValue().operatorId()).isEqualTo("admin-1");
        assertThat(terminate.getValue().reason()).isEqualTo("force stop");
        assertThat(reset.getValue().operatorId()).isEqualTo("user-1");
        assertThat(reset.getValue().reason()).isEqualTo("reset workflow");
        assertThat(approve.getValue().taskId()).isEqualTo("task-1");
        assertThat(approve.getValue().operatorId()).isEqualTo("user-1");
        assertThat(approve.getValue().selectedRouteKey()).isEqualTo("leftRoute");
        assertThat(approve.getValue().selectedReason()).isEqualTo("choose left");
        assertThat(approve.getValue().manualRouteSelections()).singleElement()
                .extracting(WorkflowManualRouteSelection::routeKey)
                .isEqualTo("routeA1");
    }

    @Test
    void shouldExposeHistoryQueriesAndDelete() {
        WorkflowHistoryInstance history = new WorkflowHistoryInstance();
        history.setId("history-1");
        history.setModuleAlias("sales.contract");
        history.setRecordId("record-1");
        WorkflowEvent event = event("event-1", WorkflowEventType.INSTANCE_TERMINATED);
        WorkflowHistoryEventView view = mock(WorkflowHistoryEventView.class);
        when(adminFacade.queryHistory(eq("sales.contract"), eq("record-1"), eq("starter-1"), any()))
                .thenReturn(List.of(history));
        when(adminFacade.renderHistoryBundle("history-1"))
                .thenReturn(new WorkflowRuntimeRenderBundle("HISTORY", null, List.of(), List.of()));
        when(adminFacade.historyEvents("history-1")).thenReturn(List.of(event));
        when(adminFacade.historyEventViews("history-1")).thenReturn(List.of(view));
        when(adminFacade.deleteHistory("history-1")).thenReturn(1);

        WebListResponse<WorkflowHistoryInstance> histories = controller.queryHistory(
                new WorkflowAdminHistoryQueryWebRequest("sales.contract", "record-1", "starter-1",
                        new WebPageRequest(2, 10)));
        WorkflowRuntimeRenderBundle bundle = controller.renderHistoryBundle("history-1", null);
        WebListResponse<WorkflowEvent> events = controller.historyEvents("history-1", null);
        WebListResponse<WorkflowHistoryEventView> eventViews = controller.historyEventViews("history-1", null);
        WebCountResponse deleted = controller.deleteHistory("history-1", null);

        assertThat(histories.records()).singleElement().extracting(WorkflowHistoryInstance::getId)
                .isEqualTo("history-1");
        assertThat(bundle.mode()).isEqualTo("HISTORY");
        assertThat(events.records()).singleElement().extracting(WorkflowEvent::getId).isEqualTo("event-1");
        assertThat(eventViews.records()).containsExactly(view);
        assertThat(deleted.count()).isEqualTo(1);
        ArgumentCaptor<PageRequest> page = ArgumentCaptor.forClass(PageRequest.class);
        verify(adminFacade).queryHistory(eq("sales.contract"), eq("record-1"), eq("starter-1"), page.capture());
        assertThat(page.getValue().getOffset()).isEqualTo(10);
        assertThat(page.getValue().getLimit()).isEqualTo(10);
        verify(adminFacade).deleteHistory("history-1");
    }

    private void assertRoute(String methodName,
                             Class<?>[] parameterTypes,
                             Class<?> httpMethod,
                             String path,
                             String actionCode) throws Exception {
        Method method = WorkflowRuntimeAdminWebController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(httpMethod.asSubclass(Annotation.class))).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo(path);
        CustomActionEndpoint endpoint = method.getAnnotation(CustomActionEndpoint.class);
        assertThat(endpoint).isNotNull();
        assertThat(endpoint.value()).isEqualTo(actionCode);
        assertThat(endpoint.level()).isEqualTo(PlatformActionLevel.LIST);
    }

    private CurrentUserContext.Scope currentUser() {
        return CurrentUserContext.use(CurrentUser.tenantUser("user-1", "User", "tenant-a"));
    }

    private WorkflowInstance instance(String id) {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(id);
        instance.setModuleAlias("sales.contract");
        instance.setRecordId("record-1");
        instance.setInstanceStatus(WorkflowInstanceStatus.RUNNING);
        return instance;
    }

    private WorkflowTask task(String id) {
        WorkflowTask task = new WorkflowTask();
        task.setId(id);
        task.setTaskKind(WorkflowTaskKind.APPROVAL);
        task.setTaskStatus(WorkflowTaskStatus.TODO);
        return task;
    }

    private WorkflowEvent event(String id, WorkflowEventType type) {
        WorkflowEvent event = new WorkflowEvent();
        event.setId(id);
        event.setEventType(type);
        return event;
    }

    private WorkflowAdminInstanceView instanceView() {
        return new WorkflowAdminInstanceView("inst-1", "sales.contract",
                "record-1", "definition-1", "version-1", 2, WorkflowInstanceStatus.RUNNING,
                WorkflowApprovalStatus.PROCESSING, "starter-1", "发起人",
                Instant.parse("2026-06-05T01:00:00Z"), List.of("approve_1"), List.of("审批"),
                List.of("task-1"), List.of("approver-1"), List.of("审批人"), WorkflowOvertimeStatus.WARNED,
                Instant.parse("2026-06-05T02:00:00Z"), Instant.parse("2026-06-05T02:30:00Z"));
    }

    private WorkflowAdminActiveTaskView activeTaskView() {
        return new WorkflowAdminActiveTaskView("task-1", "inst-1", "node-1",
                "approve_1", "审批", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO, "delegate-1", "代理人",
                Instant.parse("2026-06-05T01:00:00Z"), Instant.parse("2026-06-05T01:00:00Z"),
                WorkflowOvertimeStatus.WARNED, true,
                net.ximatai.muyun.spring.platform.workflow.WorkflowAssignmentKind.DELEGATED,
                "principal-1", "原审批人", "principal-1", "原审批人", "delegate-1", "代理人",
                true, "delegation-1", "{\"delegationPolicyId\":\"delegation-1\"}",
                true, "approve_source", "operator-1", Instant.parse("2026-06-05T00:30:00Z"));
    }
}
