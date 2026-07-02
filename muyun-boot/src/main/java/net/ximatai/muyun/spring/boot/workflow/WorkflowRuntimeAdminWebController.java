package net.ximatai.muyun.spring.boot.workflow;

import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.platform.workflow.WorkflowAdminFacade;
import net.ximatai.muyun.spring.platform.workflow.WorkflowActionPolicyService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowAdminActiveTaskView;
import net.ximatai.muyun.spring.platform.workflow.WorkflowAdminInstanceQueryRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowAdminInstanceView;
import net.ximatai.muyun.spring.platform.workflow.WorkflowApprovalStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowEvent;
import net.ximatai.muyun.spring.platform.workflow.WorkflowHistoryEventView;
import net.ximatai.muyun.spring.platform.workflow.WorkflowHistoryInstance;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceActionRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceActionResult;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowManualRouteSelection;
import net.ximatai.muyun.spring.platform.workflow.WorkflowOvertimeStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRuntimeRenderBundle;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTask;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskActionRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskActionResult;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Path("/workflow/runtime/admin")
@PlatformStaticModule(application = "platform",
        alias = WorkflowActionPolicyService.MANAGEMENT_MODULE_ALIAS,
        title = "Workflow Admin")
@PlatformMenu(parent = PlatformMenuGroups.OPS, title = "工作流运维", order = 20)
public class WorkflowRuntimeAdminWebController {
    private final WorkflowAdminFacade adminFacade;

    public WorkflowRuntimeAdminWebController(WorkflowAdminFacade adminFacade) {
        this.adminFacade = adminFacade;
    }

    @GET
    @Path("/instance/{instanceId}/todo-tasks")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_TODO_TASK_QUERY_ACTION,
            title = "Todo Task Query", level = PlatformActionLevel.LIST)
    public WebListResponse<WorkflowTask> currentTodoTasks(@PathParam("instanceId") String instanceId) {
        return new WebListResponse<>(adminFacade.currentTodoTasks(instanceId));
    }

    @GET
    @Path("/instance/{instanceId}/active-tasks")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_FORCE_APPROVE_ACTION,
            title = "Force Handle Candidate Query", level = PlatformActionLevel.LIST)
    public WebListResponse<WorkflowAdminActiveTaskView> currentTodoTaskViews(@PathParam("instanceId") String instanceId) {
        return new WebListResponse<>(adminFacade.currentTodoTaskViews(instanceId));
    }

    @POST
    @Path("/instance/query")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION,
            title = "Workflow Admin Query", level = PlatformActionLevel.LIST)
    public WebListResponse<WorkflowAdminInstanceView> queryCurrentInstances(
            WorkflowAdminInstanceQueryWebRequest request) {
        WorkflowAdminInstanceQueryWebRequest payload = request == null
                ? new WorkflowAdminInstanceQueryWebRequest(null, null, null, null, null, null, null, null)
                : request;
        return new WebListResponse<>(adminFacade.queryCurrentInstances(payload.toServiceRequest(),
                page(payload.page())));
    }

    @POST
    @Path("/instance/{instanceId}/bundle")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION,
            title = "Workflow Admin Query", level = PlatformActionLevel.LIST)
    public WorkflowRuntimeRenderBundle renderCurrentBundle(@PathParam("instanceId") String instanceId,
                                                           Object ignored) {
        return adminFacade.renderCurrentBundle(instanceId);
    }

    @POST
    @Path("/instance/{instanceId}/events")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION,
            title = "Workflow Admin Query", level = PlatformActionLevel.LIST)
    public WebListResponse<WorkflowEvent> currentEvents(@PathParam("instanceId") String instanceId,
                                                        Object ignored) {
        return new WebListResponse<>(adminFacade.currentEvents(instanceId));
    }

    @POST
    @Path("/instance/{instanceId}/tasks")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION,
            title = "Workflow Admin Query", level = PlatformActionLevel.LIST)
    public WebListResponse<WorkflowTask> currentTasks(@PathParam("instanceId") String instanceId,
                                                      Object ignored) {
        return new WebListResponse<>(adminFacade.currentTasks(instanceId));
    }

    @POST
    @Path("/instance/{instanceId}/actions/forceTerminate")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_FORCE_TERMINATE_ACTION,
            title = "Force Terminate", level = PlatformActionLevel.LIST)
    public WorkflowInstanceActionResult forceTerminate(
            @PathParam("instanceId") String instanceId,
            WorkflowAdminActionWebRequest request) {
        return adminFacade.forceTerminate(new WorkflowInstanceActionRequest(instanceId,
                operatorId(request == null ? null : request.operatorId()),
                request == null ? null : request.reason(),
                null));
    }

    @POST
    @Path("/instance/{instanceId}/actions/reset")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_RESET_ACTION,
            title = "Reset Workflow", level = PlatformActionLevel.LIST)
    public WorkflowInstanceActionResult reset(
            @PathParam("instanceId") String instanceId,
            WorkflowAdminActionWebRequest request) {
        return adminFacade.reset(new WorkflowInstanceActionRequest(instanceId,
                operatorId(request == null ? null : request.operatorId()),
                request == null ? null : request.reason(),
                null));
    }

    @POST
    @Path("/task/{taskId}/actions/forceApprove")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_FORCE_APPROVE_ACTION,
            title = "Force Handle", level = PlatformActionLevel.LIST)
    public WorkflowTaskActionResult forceApprove(
            @PathParam("taskId") String taskId,
            WorkflowAdminTaskActionWebRequest request) {
        return adminFacade.forceApprove(new WorkflowTaskActionRequest(taskId,
                operatorId(request == null ? null : request.operatorId()),
                null,
                null,
                null,
                null,
                request == null ? null : request.reason(),
                null,
                request == null ? null : request.selectedRouteKeyOrDirectLinkKey(),
                request == null ? null : request.selectedReason(),
                request == null ? null : request.manualRouteSelections()));
    }

    @POST
    @Path("/history/query")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION,
            title = "Workflow Admin Query", level = PlatformActionLevel.LIST)
    public WebListResponse<WorkflowHistoryInstance> queryHistory(
            WorkflowAdminHistoryQueryWebRequest request) {
        WorkflowAdminHistoryQueryWebRequest payload = request == null
                ? new WorkflowAdminHistoryQueryWebRequest(null, null, null, null)
                : request;
        return new WebListResponse<>(adminFacade.queryHistory(
                payload.moduleAlias(), payload.recordId(), payload.startedBy(), page(payload.page())));
    }

    @POST
    @Path("/history/{historyInstanceId}/bundle")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION,
            title = "Workflow Admin Query", level = PlatformActionLevel.LIST)
    public WorkflowRuntimeRenderBundle renderHistoryBundle(@PathParam("historyInstanceId") String historyInstanceId,
                                                           Object ignored) {
        return adminFacade.renderHistoryBundle(historyInstanceId);
    }

    @POST
    @Path("/history/{historyInstanceId}/events")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION,
            title = "Workflow Admin Query", level = PlatformActionLevel.LIST)
    public WebListResponse<WorkflowEvent> historyEvents(@PathParam("historyInstanceId") String historyInstanceId,
                                                        Object ignored) {
        return new WebListResponse<>(adminFacade.historyEvents(historyInstanceId));
    }

    @POST
    @Path("/history/{historyInstanceId}/events/view")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION,
            title = "Workflow Admin Query", level = PlatformActionLevel.LIST)
    public WebListResponse<WorkflowHistoryEventView> historyEventViews(@PathParam("historyInstanceId") String historyInstanceId,
                                                                       Object ignored) {
        return new WebListResponse<>(adminFacade.historyEventViews(historyInstanceId));
    }

    @POST
    @Path("/history/{historyInstanceId}/delete")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_DELETE_HISTORY_ACTION,
            title = "Delete Workflow History", level = PlatformActionLevel.LIST)
    public WebCountResponse deleteHistory(@PathParam("historyInstanceId") String historyInstanceId,
                                          Object ignored) {
        return new WebCountResponse(adminFacade.deleteHistory(historyInstanceId));
    }

    private String operatorId(String operatorId) {
        if (operatorId != null && !operatorId.isBlank()) {
            return operatorId;
        }
        return CurrentUserContext.currentUser()
                .map(user -> user.userId())
                .filter(userId -> !userId.isBlank())
                .orElseThrow(() -> new PlatformException("workflow operator id must not be blank"));
    }

    private PageRequest page(WebPageRequest request) {
        WebPageRequest normalized = request == null ? WebPageRequest.DEFAULT : request;
        return PageRequest.of(normalized.pageNum(), normalized.pageSize());
    }
}

record WorkflowAdminActionWebRequest(String operatorId, String reason) {
}

record WorkflowAdminTaskActionWebRequest(String operatorId,
                                         String reason,
                                         String selectedRouteKey,
                                         String selectedDirectLinkKey,
                                         String selectedReason,
                                         java.util.List<WorkflowManualRouteSelection> manualRouteSelections) {
    String selectedRouteKeyOrDirectLinkKey() {
        return selectedRouteKey == null || selectedRouteKey.isBlank() ? selectedDirectLinkKey : selectedRouteKey;
    }
}

record WorkflowAdminHistoryQueryWebRequest(String moduleAlias, String recordId, String startedBy, WebPageRequest page) {
}

record WorkflowAdminInstanceQueryWebRequest(
        String moduleAlias,
        String recordId,
        String starterId,
        WorkflowInstanceStatus instanceStatus,
        WorkflowApprovalStatus approvalStatus,
        String currentAssigneeId,
        WorkflowOvertimeStatus overtimeStatus,
        WebPageRequest page
) {
    WorkflowAdminInstanceQueryRequest toServiceRequest() {
        return new WorkflowAdminInstanceQueryRequest(moduleAlias, recordId, starterId, instanceStatus, approvalStatus,
                currentAssigneeId, overtimeStatus);
    }
}
