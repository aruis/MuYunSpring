package net.ximatai.muyun.spring.boot.workflow;

import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
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
import net.ximatai.muyun.spring.platform.workflow.WorkflowOvertimeStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRuntimeRenderBundle;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTask;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskActionRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskActionResult;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workflow/runtime/admin")
@PlatformStaticModule(application = "platform",
        alias = WorkflowActionPolicyService.MANAGEMENT_MODULE_ALIAS,
        title = "Workflow Admin")
public class WorkflowRuntimeAdminWebController {
    private final WorkflowAdminFacade adminFacade;

    public WorkflowRuntimeAdminWebController(WorkflowAdminFacade adminFacade) {
        this.adminFacade = adminFacade;
    }

    @GetMapping("/instance/{instanceId}/todo-tasks")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_TODO_TASK_QUERY_ACTION,
            title = "Todo Task Query", level = PlatformActionLevel.LIST)
    public WebListResponse<WorkflowTask> currentTodoTasks(@PathVariable String instanceId) {
        return new WebListResponse<>(adminFacade.currentTodoTasks(instanceId));
    }

    @GetMapping("/instance/{instanceId}/active-tasks")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_TODO_TASK_QUERY_ACTION,
            title = "Active Task Query", level = PlatformActionLevel.LIST)
    public WebListResponse<WorkflowAdminActiveTaskView> currentTodoTaskViews(@PathVariable String instanceId) {
        return new WebListResponse<>(adminFacade.currentTodoTaskViews(instanceId));
    }

    @PostMapping("/instance/query")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION,
            title = "Workflow Admin Query", level = PlatformActionLevel.LIST)
    public WebListResponse<WorkflowAdminInstanceView> queryCurrentInstances(
            @RequestBody(required = false) WorkflowAdminInstanceQueryWebRequest request) {
        WorkflowAdminInstanceQueryWebRequest payload = request == null
                ? new WorkflowAdminInstanceQueryWebRequest(null, null, null, null, null, null, null, null)
                : request;
        return new WebListResponse<>(adminFacade.queryCurrentInstances(payload.toServiceRequest(),
                page(payload.page())));
    }

    @PostMapping({"/instance/{instanceId}/bundle", "/instance/{instanceId}/render"})
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION,
            title = "Workflow Admin Query", level = PlatformActionLevel.LIST)
    public WorkflowRuntimeRenderBundle renderCurrentBundle(@PathVariable String instanceId,
                                                           @RequestBody(required = false) Object ignored) {
        return adminFacade.renderCurrentBundle(instanceId);
    }

    @PostMapping("/instance/{instanceId}/events")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION,
            title = "Workflow Admin Query", level = PlatformActionLevel.LIST)
    public WebListResponse<WorkflowEvent> currentEvents(@PathVariable String instanceId,
                                                        @RequestBody(required = false) Object ignored) {
        return new WebListResponse<>(adminFacade.currentEvents(instanceId));
    }

    @PostMapping("/instance/{instanceId}/tasks")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION,
            title = "Workflow Admin Query", level = PlatformActionLevel.LIST)
    public WebListResponse<WorkflowTask> currentTasks(@PathVariable String instanceId,
                                                      @RequestBody(required = false) Object ignored) {
        return new WebListResponse<>(adminFacade.currentTasks(instanceId));
    }

    @PostMapping("/instance/{instanceId}/actions/forceTerminate")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_FORCE_TERMINATE_ACTION,
            title = "Force Terminate", level = PlatformActionLevel.LIST)
    public WorkflowInstanceActionResult forceTerminate(
            @PathVariable String instanceId,
            @RequestBody(required = false) WorkflowAdminActionWebRequest request) {
        return adminFacade.forceTerminate(new WorkflowInstanceActionRequest(instanceId,
                operatorId(request == null ? null : request.operatorId()),
                request == null ? null : request.reason(),
                null));
    }

    @PostMapping("/task/{taskId}/actions/forceApprove")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_FORCE_APPROVE_ACTION,
            title = "Force Approve", level = PlatformActionLevel.LIST)
    public WorkflowTaskActionResult forceApprove(
            @PathVariable String taskId,
            @RequestBody(required = false) WorkflowAdminActionWebRequest request) {
        return adminFacade.forceApprove(new WorkflowTaskActionRequest(taskId,
                operatorId(request == null ? null : request.operatorId()),
                null,
                null,
                request == null ? null : request.reason(),
                null));
    }

    @PostMapping("/history/query")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION,
            title = "Workflow Admin Query", level = PlatformActionLevel.LIST)
    public WebListResponse<WorkflowHistoryInstance> queryHistory(
            @RequestBody(required = false) WorkflowAdminHistoryQueryWebRequest request) {
        WorkflowAdminHistoryQueryWebRequest payload = request == null
                ? new WorkflowAdminHistoryQueryWebRequest(null, null, null)
                : request;
        return new WebListResponse<>(adminFacade.queryHistory(
                payload.moduleAlias(), payload.recordId(), page(payload.page())));
    }

    @PostMapping({"/history/{historyInstanceId}/bundle", "/history/{historyInstanceId}/render"})
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION,
            title = "Workflow Admin Query", level = PlatformActionLevel.LIST)
    public WorkflowRuntimeRenderBundle renderHistoryBundle(@PathVariable String historyInstanceId,
                                                           @RequestBody(required = false) Object ignored) {
        return adminFacade.renderHistoryBundle(historyInstanceId);
    }

    @PostMapping("/history/{historyInstanceId}/events")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION,
            title = "Workflow Admin Query", level = PlatformActionLevel.LIST)
    public WebListResponse<WorkflowEvent> historyEvents(@PathVariable String historyInstanceId,
                                                        @RequestBody(required = false) Object ignored) {
        return new WebListResponse<>(adminFacade.historyEvents(historyInstanceId));
    }

    @PostMapping("/history/{historyInstanceId}/events/view")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION,
            title = "Workflow Admin Query", level = PlatformActionLevel.LIST)
    public WebListResponse<WorkflowHistoryEventView> historyEventViews(@PathVariable String historyInstanceId,
                                                                       @RequestBody(required = false) Object ignored) {
        return new WebListResponse<>(adminFacade.historyEventViews(historyInstanceId));
    }

    @PostMapping("/history/{historyInstanceId}/delete")
    @CustomActionEndpoint(value = WorkflowActionPolicyService.MANAGEMENT_DELETE_HISTORY_ACTION,
            title = "Delete Workflow History", level = PlatformActionLevel.LIST)
    public WebCountResponse deleteHistory(@PathVariable String historyInstanceId,
                                          @RequestBody(required = false) Object ignored) {
        return new WebCountResponse(adminFacade.deleteHistory(historyInstanceId));
    }

    @ExceptionHandler({IllegalArgumentException.class, PlatformException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public WorkflowWebError handleBadRequest(RuntimeException exception) {
        return new WorkflowWebError("bad_request", exception.getMessage());
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

record WorkflowAdminHistoryQueryWebRequest(String moduleAlias, String recordId, WebPageRequest page) {
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
