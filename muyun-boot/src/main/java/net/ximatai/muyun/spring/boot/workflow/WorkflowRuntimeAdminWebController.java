package net.ximatai.muyun.spring.boot.workflow;

import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.platform.workflow.WorkflowAdminFacade;
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
public class WorkflowRuntimeAdminWebController {
    private final WorkflowAdminFacade adminFacade;

    public WorkflowRuntimeAdminWebController(WorkflowAdminFacade adminFacade) {
        this.adminFacade = adminFacade;
    }

    @GetMapping("/instance/{instanceId}/todo-tasks")
    public WebListResponse<WorkflowTask> currentTodoTasks(@PathVariable String instanceId) {
        return new WebListResponse<>(adminFacade.currentTodoTasks(instanceId));
    }

    @PostMapping("/instance/{instanceId}/actions/forceTerminate")
    public WorkflowInstanceActionResult forceTerminate(
            @PathVariable String instanceId,
            @RequestBody(required = false) WorkflowAdminActionWebRequest request) {
        return adminFacade.forceTerminate(new WorkflowInstanceActionRequest(instanceId,
                operatorId(request == null ? null : request.operatorId()),
                request == null ? null : request.reason(),
                null));
    }

    @PostMapping("/task/{taskId}/actions/forceApprove")
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
