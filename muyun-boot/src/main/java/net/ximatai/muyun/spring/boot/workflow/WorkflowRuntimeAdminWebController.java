package net.ximatai.muyun.spring.boot.workflow;

import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.platform.workflow.WorkflowAdminFacade;
import net.ximatai.muyun.spring.platform.workflow.WorkflowActionPolicyService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceActionRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceActionResult;
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
}

record WorkflowAdminActionWebRequest(String operatorId, String reason) {
}
