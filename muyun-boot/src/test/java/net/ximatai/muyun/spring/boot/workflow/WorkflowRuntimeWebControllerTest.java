package net.ximatai.muyun.spring.boot.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstance;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceActionFacade;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceActionRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceActionResult;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowManualRouteSelection;
import net.ximatai.muyun.spring.platform.workflow.WorkflowModuleTaskCompletionPolicy;
import net.ximatai.muyun.spring.platform.workflow.WorkflowModuleTaskContext;
import net.ximatai.muyun.spring.platform.workflow.WorkflowModuleTaskContinueResult;
import net.ximatai.muyun.spring.platform.workflow.WorkflowModuleTaskEvaluation;
import net.ximatai.muyun.spring.platform.workflow.WorkflowModuleTaskProcessBundle;
import net.ximatai.muyun.spring.platform.workflow.WorkflowModuleTaskRuntimeService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRejectResubmitMode;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRuntimeReadFacade;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRuntimeRenderBundle;
import net.ximatai.muyun.spring.platform.workflow.WorkflowSubmitFacade;
import net.ximatai.muyun.spring.platform.workflow.WorkflowSubmitPreviewView;
import net.ximatai.muyun.spring.platform.workflow.WorkflowSubmitReadFacade;
import net.ximatai.muyun.spring.platform.workflow.WorkflowSubmitRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowSubmitResult;
import net.ximatai.muyun.spring.platform.workflow.WorkflowSubmitStatusView;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTask;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskActionFacade;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskActionRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskActionResult;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskDefinition;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskKind;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowWorkbenchCard;
import net.ximatai.muyun.spring.platform.workflow.WorkflowWorkbenchQueryRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowWorkbenchSort;
import net.ximatai.muyun.spring.platform.workflow.WorkflowWorkbenchStats;
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

class WorkflowRuntimeWebControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private WorkflowRuntimeReadFacade runtimeReadFacade;
    private WorkflowTaskActionFacade taskActionFacade;
    private WorkflowInstanceActionFacade instanceActionFacade;
    private WorkflowModuleTaskRuntimeService moduleTaskRuntimeService;
    private WorkflowSubmitFacade submitFacade;
    private WorkflowSubmitReadFacade submitReadFacade;
    private WorkflowRuntimeWebController controller;

    @BeforeEach
    void setUp() {
        runtimeReadFacade = mock(WorkflowRuntimeReadFacade.class);
        taskActionFacade = mock(WorkflowTaskActionFacade.class);
        instanceActionFacade = mock(WorkflowInstanceActionFacade.class);
        moduleTaskRuntimeService = mock(WorkflowModuleTaskRuntimeService.class);
        submitFacade = mock(WorkflowSubmitFacade.class);
        submitReadFacade = mock(WorkflowSubmitReadFacade.class);
        controller = new WorkflowRuntimeWebController(runtimeReadFacade, taskActionFacade,
                instanceActionFacade, moduleTaskRuntimeService, submitFacade, submitReadFacade);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        TenantContext.clear();
    }

    @Test
    void shouldDeclareWorkflowRuntimeRoutesWithJaxRsAnnotations() throws Exception {
        assertThat(WorkflowRuntimeWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/workflow/runtime");
        assertRoute("renderBundle", new Class<?>[]{String.class},
                GET.class, "/instance/{instanceId}/bundle");
        assertRoute("submitStatus", new Class<?>[]{String.class, String.class, WorkflowSubmitWebRequest.class},
                POST.class, "/record/{moduleAlias}/{recordId}/submit/status");
        assertRoute("submitApproval", new Class<?>[]{String.class, String.class, WorkflowSubmitWebRequest.class},
                POST.class, "/record/{moduleAlias}/{recordId}/actions/submitApproval");
        assertRoute("executeInstanceAction",
                new Class<?>[]{String.class, String.class, WorkflowInstanceActionWebRequest.class},
                POST.class, "/instance/{instanceId}/actions/{actionCode}");
        assertRoute("executeTaskAction", new Class<?>[]{String.class, String.class, WorkflowTaskActionWebRequest.class},
                POST.class, "/task/{taskId}/actions/{actionCode}");
        assertRoute("todoCards", new Class<?>[]{WorkflowWorkbenchWebRequest.class},
                POST.class, "/workbench/todo/query");
        assertRoute("checkAndContinueModuleTask",
                new Class<?>[]{String.class, WorkflowModuleTaskContinueWebRequest.class},
                POST.class, "/task/{taskId}/module-task/check-and-continue");

        Method submitApproval = WorkflowRuntimeWebController.class.getMethod("submitApproval",
                String.class, String.class, WorkflowSubmitWebRequest.class);
        CustomActionEndpoint endpoint = submitApproval.getAnnotation(CustomActionEndpoint.class);
        assertThat(endpoint).isNotNull();
        assertThat(endpoint.value()).isEqualTo("submitApproval");
        assertThat(endpoint.level()).isEqualTo(PlatformActionLevel.RECORD);
        assertThat(endpoint.recordIdPathVariable()).isEqualTo("recordId");
        assertThat(endpoint.dataAuth()).isTrue();
    }

    @Test
    void shouldExposeRuntimeReadDelegates() {
        WorkflowInstance instance = instance("inst-1");
        when(runtimeReadFacade.renderBundle("inst-1"))
                .thenReturn(new WorkflowRuntimeRenderBundle("RUNTIME", instance, List.of(), List.of()));

        WorkflowRuntimeRenderBundle bundle = controller.renderBundle("inst-1");

        assertThat(bundle.mode()).isEqualTo("RUNTIME");
        assertThat(bundle.instance().getId()).isEqualTo("inst-1");
        verify(runtimeReadFacade).renderBundle("inst-1");
    }

    @Test
    void shouldBuildSubmitRequestsFromCurrentUser() {
        WorkflowSubmitStatusView status = mock(WorkflowSubmitStatusView.class);
        WorkflowSubmitPreviewView preview = mock(WorkflowSubmitPreviewView.class);
        WorkflowSubmitResult submitResult = mock(WorkflowSubmitResult.class);
        when(submitReadFacade.status(any())).thenReturn(status);
        when(submitReadFacade.preview(any())).thenReturn(preview);
        when(submitFacade.submit(any())).thenReturn(submitResult);

        WorkflowSubmitWebRequest request = new WorkflowSubmitWebRequest(
                "ignored-operator",
                "org-1",
                null,
                "leftRoute",
                "choose left",
                List.of(new WorkflowManualRouteSelection("branchA", "routeA1", "choose A1"))
        );
        WorkflowSubmitStatusView statusResult;
        WorkflowSubmitPreviewView previewResult;
        WorkflowSubmitResult approvalResult;
        try (CurrentUserContext.Scope ignored = currentUser()) {
            statusResult = controller.submitStatus("sales.contract", "record-1", request);
            previewResult = controller.submitPreview("sales.contract", "record-1", request);
            approvalResult = controller.submitApproval("sales.contract", "record-1", request);
        }

        assertThat(statusResult).isSameAs(status);
        assertThat(previewResult).isSameAs(preview);
        assertThat(approvalResult).isSameAs(submitResult);
        ArgumentCaptor<WorkflowSubmitRequest> statusRequest = ArgumentCaptor.forClass(WorkflowSubmitRequest.class);
        ArgumentCaptor<WorkflowSubmitRequest> previewRequest = ArgumentCaptor.forClass(WorkflowSubmitRequest.class);
        ArgumentCaptor<WorkflowSubmitRequest> submitRequest = ArgumentCaptor.forClass(WorkflowSubmitRequest.class);
        verify(submitReadFacade).status(statusRequest.capture());
        verify(submitReadFacade).preview(previewRequest.capture());
        verify(submitFacade).submit(submitRequest.capture());
        assertSubmitRequest(statusRequest.getValue(), "sales.contract", "record-1", "user-1", "leftRoute");
        assertSubmitRequest(previewRequest.getValue(), "sales.contract", "record-1", "user-1", "leftRoute");
        assertSubmitRequest(submitRequest.getValue(), "sales.contract", "record-1", "user-1", "leftRoute");
        assertThat(submitRequest.getValue().approvalRequired()).isTrue();
        assertThat(submitRequest.getValue().manualRouteSelections()).singleElement()
                .extracting(WorkflowManualRouteSelection::routeKey)
                .isEqualTo("routeA1");
    }

    @Test
    void shouldExecuteInstanceAndTaskActionsWithCurrentUser() throws Exception {
        WorkflowInstanceActionResult instanceResult = mock(WorkflowInstanceActionResult.class);
        WorkflowTaskActionResult taskResult = mock(WorkflowTaskActionResult.class);
        when(instanceActionFacade.execute(eq("revoke"), any())).thenReturn(instanceResult);
        when(taskActionFacade.execute(eq("approve"), any())).thenReturn(taskResult);

        JsonNode semanticJson = objectMapper.readTree("{\"nodes\":[\"approve\"]}");
        JsonNode layoutJson = objectMapper.readTree("{\"zoom\":1}");
        WorkflowInstanceActionResult actualInstanceResult;
        WorkflowTaskActionResult actualTaskResult;
        try (CurrentUserContext.Scope ignored = currentUser()) {
            actualInstanceResult = controller.executeInstanceAction("inst-1", "revoke",
                    new WorkflowInstanceActionWebRequest("ignored-operator", "cancel"));
            actualTaskResult = controller.executeTaskAction("task-1", "approve",
                    new WorkflowTaskActionWebRequest(
                            "ignored-operator",
                            "user-2",
                            null,
                            "return_to_me",
                            "approved",
                            null,
                            "leftRoute",
                            "choose left",
                            List.of(new WorkflowManualRouteSelection("branchA", "routeA1", "choose A1")),
                            semanticJson,
                            layoutJson
                    ));
        }

        assertThat(actualInstanceResult).isSameAs(instanceResult);
        assertThat(actualTaskResult).isSameAs(taskResult);
        ArgumentCaptor<WorkflowInstanceActionRequest> instanceRequest =
                ArgumentCaptor.forClass(WorkflowInstanceActionRequest.class);
        ArgumentCaptor<WorkflowTaskActionRequest> taskRequest =
                ArgumentCaptor.forClass(WorkflowTaskActionRequest.class);
        verify(instanceActionFacade).execute(eq("revoke"), instanceRequest.capture());
        verify(taskActionFacade).execute(eq("approve"), taskRequest.capture());
        assertThat(instanceRequest.getValue().instanceId()).isEqualTo("inst-1");
        assertThat(instanceRequest.getValue().operatorId()).isEqualTo("user-1");
        assertThat(instanceRequest.getValue().reason()).isEqualTo("cancel");
        assertThat(taskRequest.getValue().taskId()).isEqualTo("task-1");
        assertThat(taskRequest.getValue().operatorId()).isEqualTo("user-1");
        assertThat(taskRequest.getValue().targetAssigneeId()).isEqualTo("user-2");
        assertThat(taskRequest.getValue().rejectResubmitMode()).isEqualTo(WorkflowRejectResubmitMode.RETURN_TO_ME);
        assertThat(taskRequest.getValue().selectedRouteKey()).isEqualTo("leftRoute");
        assertThat(taskRequest.getValue().selectedReason()).isEqualTo("choose left");
        assertThat(taskRequest.getValue().manualRouteSelections()).singleElement()
                .extracting(WorkflowManualRouteSelection::branchNodeKey)
                .isEqualTo("branchA");
        assertThat(taskRequest.getValue().semanticJson()).isEqualTo("{\"nodes\":[\"approve\"]}");
        assertThat(taskRequest.getValue().layoutJson()).isEqualTo("{\"zoom\":1}");
    }

    @Test
    void shouldQueryWorkbenchWithNormalizedPageAndCurrentUser() {
        WorkflowWorkbenchCard card = mock(WorkflowWorkbenchCard.class);
        when(runtimeReadFacade.todoCards(eq("user-1"), any(), any())).thenReturn(List.of(card));
        when(runtimeReadFacade.workbenchStats(eq("todo"), eq("user-1"), any()))
                .thenReturn(mock(WorkflowWorkbenchStats.class));
        WorkflowWorkbenchWebRequest request = new WorkflowWorkbenchWebRequest(
                "ignored-operator",
                new WebPageRequest(2, 30),
                "crm.contract",
                "record-1",
                "def-1",
                "workflow-ver-1",
                "def-ver-1",
                WorkflowInstanceStatus.RUNNING,
                "approve",
                WorkflowTaskKind.APPROVAL,
                WorkflowTaskStatus.TODO,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Boolean.TRUE,
                "origin-approve",
                "starter-1",
                List.of(new WorkflowWorkbenchSort("receivedAt",
                        net.ximatai.muyun.spring.platform.workflow.WorkflowSortDirection.ASC))
        );

        WebListResponse<WorkflowWorkbenchCard> cards;
        WorkflowWorkbenchStats stats;
        try (CurrentUserContext.Scope ignored = currentUser()) {
            cards = controller.todoCards(request);
            stats = controller.workbenchStats("todo", request);
        }

        assertThat(cards.records()).containsExactly(card);
        assertThat(stats).isNotNull();
        ArgumentCaptor<PageRequest> page = ArgumentCaptor.forClass(PageRequest.class);
        ArgumentCaptor<WorkflowWorkbenchQueryRequest> query = ArgumentCaptor.forClass(WorkflowWorkbenchQueryRequest.class);
        verify(runtimeReadFacade).todoCards(eq("user-1"), page.capture(), query.capture());
        assertThat(page.getValue().getOffset()).isEqualTo(30);
        assertThat(page.getValue().getLimit()).isEqualTo(30);
        assertThat(query.getValue().moduleAlias()).isEqualTo("crm.contract");
        assertThat(query.getValue().recordId()).isEqualTo("record-1");
        assertThat(query.getValue().instanceStatus()).isEqualTo(WorkflowInstanceStatus.RUNNING);
        assertThat(query.getValue().nodeKey()).isEqualTo("approve");
        assertThat(query.getValue().taskKind()).isEqualTo(WorkflowTaskKind.APPROVAL);
        assertThat(query.getValue().taskStatus()).isEqualTo(WorkflowTaskStatus.TODO);
        assertThat(query.getValue().addedByAddSign()).isTrue();
        assertThat(query.getValue().addSignSourceNodeKey()).isEqualTo("origin-approve");
        assertThat(query.getValue().submitterUserId()).isEqualTo("starter-1");
        assertThat(query.getValue().sorts()).singleElement()
                .extracting(WorkflowWorkbenchSort::field)
                .isEqualTo("receivedAt");
    }

    @Test
    void shouldPrepareAndContinueModuleTaskWithManualRoutes() {
        WorkflowModuleTaskProcessBundle bundle = moduleTaskBundle("task-1");
        WorkflowModuleTaskContinueResult continueResult = WorkflowModuleTaskContinueResult.continued(
                WorkflowTaskActionResult.of(new WorkflowTask(), null));
        when(moduleTaskRuntimeService.prepare("task-1", "user-1")).thenReturn(bundle);
        when(moduleTaskRuntimeService.checkAndContinue(eq("task-1"), eq("user-1"), eq("done"),
                eq(null), eq(null), any())).thenReturn(continueResult);

        WorkflowModuleTaskProcessBundle actualBundle;
        WorkflowModuleTaskContinueResult actualContinueResult;
        try (CurrentUserContext.Scope ignored = currentUser()) {
            actualBundle = controller.prepareModuleTask("task-1");
            actualContinueResult = controller.checkAndContinueModuleTask("task-1",
                    new WorkflowModuleTaskContinueWebRequest(
                            "ignored-operator",
                            "done",
                            null,
                            null,
                            null,
                            List.of(
                                    new WorkflowManualRouteSelection("branchA", "routeA1", "choose A1"),
                                    new WorkflowManualRouteSelection("branchB", "routeB2", "choose B2")
                            )
                    ));
        }

        assertThat(actualBundle.taskId()).isEqualTo("task-1");
        assertThat(actualContinueResult).isSameAs(continueResult);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WorkflowManualRouteSelection>> selections = ArgumentCaptor.forClass(List.class);
        verify(moduleTaskRuntimeService).prepare("task-1", "user-1");
        verify(moduleTaskRuntimeService).checkAndContinue(eq("task-1"), eq("user-1"), eq("done"),
                eq(null), eq(null), selections.capture());
        assertThat(selections.getValue()).extracting(WorkflowManualRouteSelection::routeKey)
                .containsExactly("routeA1", "routeB2");
    }

    private CurrentUserContext.Scope currentUser() {
        return CurrentUserContext.use(CurrentUser.tenantUser("user-1", "User", "tenant-a"));
    }

    private void assertRoute(String methodName, Class<?>[] parameterTypes, Class<?> httpMethod, String path)
            throws Exception {
        Method method = WorkflowRuntimeWebController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(httpMethod.asSubclass(Annotation.class))).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo(path);
    }

    private void assertSubmitRequest(WorkflowSubmitRequest request,
                                     String moduleAlias,
                                     String recordId,
                                     String operatorId,
                                     String selectedRouteKey) {
        assertThat(request.moduleAlias()).isEqualTo(moduleAlias);
        assertThat(request.recordId()).isEqualTo(recordId);
        assertThat(request.operatorId()).isEqualTo(operatorId);
        assertThat(request.authOrgId()).isEqualTo("org-1");
        assertThat(request.selectedRouteKey()).isEqualTo(selectedRouteKey);
        assertThat(request.selectedReason()).isEqualTo("choose left");
    }

    private WorkflowInstance instance(String id) {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(id);
        instance.setDefinitionId("def-1");
        instance.setWorkflowVersionId("ver-1");
        instance.setVersionNo(1);
        instance.setModuleAlias("crm.contract");
        instance.setRecordId("record-1");
        instance.setInstanceStatus(WorkflowInstanceStatus.RUNNING);
        instance.setSnapshotText("{}");
        return instance;
    }

    private WorkflowModuleTaskProcessBundle moduleTaskBundle(String taskId) {
        WorkflowTaskDefinition definition = new WorkflowTaskDefinition();
        definition.setId("task-def-1");
        definition.setModuleAlias("crm.contract");
        definition.setAlias("visit");
        return new WorkflowModuleTaskProcessBundle(taskId, "inst-1", "visit", "crm.contract", "record-1",
                WorkflowModuleTaskCompletionPolicy.MANUAL_CONFIRM,
                new WorkflowModuleTaskContext(taskId, WorkflowModuleTaskCompletionPolicy.MANUAL_CONFIRM,
                        "/workflow/runtime/task/" + taskId + "/module-task/check-and-continue"),
                definition,
                WorkflowModuleTaskEvaluation.manualConfirm(List.of()),
                null);
    }
}
