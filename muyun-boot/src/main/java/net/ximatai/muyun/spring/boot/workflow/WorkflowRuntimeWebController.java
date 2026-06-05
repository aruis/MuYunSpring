package net.ximatai.muyun.spring.boot.workflow;

import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.platform.workflow.WorkflowEvent;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceActionFacade;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceActionRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceActionResult;
import net.ximatai.muyun.spring.platform.workflow.WorkflowModuleTaskContinueResult;
import net.ximatai.muyun.spring.platform.workflow.WorkflowModuleTaskProcessBundle;
import net.ximatai.muyun.spring.platform.workflow.WorkflowModuleTaskRuntimeService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRejectResubmitMode;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRuntimeReadFacade;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRuntimeRenderBundle;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTask;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskActionFacade;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskActionRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskActionResult;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskAvailableAction;
import net.ximatai.muyun.spring.platform.workflow.WorkflowWorkbenchCard;
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
@RequestMapping("/workflow/runtime")
public class WorkflowRuntimeWebController {
    private final WorkflowRuntimeReadFacade runtimeReadFacade;
    private final WorkflowTaskActionFacade taskActionFacade;
    private final WorkflowInstanceActionFacade instanceActionFacade;
    private final WorkflowModuleTaskRuntimeService moduleTaskRuntimeService;

    public WorkflowRuntimeWebController(WorkflowRuntimeReadFacade runtimeReadFacade,
                                        WorkflowTaskActionFacade taskActionFacade,
                                        WorkflowInstanceActionFacade instanceActionFacade,
                                        WorkflowModuleTaskRuntimeService moduleTaskRuntimeService) {
        this.runtimeReadFacade = runtimeReadFacade;
        this.taskActionFacade = taskActionFacade;
        this.instanceActionFacade = instanceActionFacade;
        this.moduleTaskRuntimeService = moduleTaskRuntimeService;
    }

    @GetMapping("/instance/{instanceId}/bundle")
    public WorkflowRuntimeRenderBundle renderBundle(@PathVariable String instanceId) {
        return runtimeReadFacade.renderBundle(instanceId);
    }

    @GetMapping("/instance/{instanceId}/tasks")
    public WebListResponse<WorkflowTask> instanceTasks(@PathVariable String instanceId) {
        return new WebListResponse<>(runtimeReadFacade.instanceTasks(instanceId));
    }

    @GetMapping("/instance/{instanceId}/events")
    public WebListResponse<WorkflowEvent> instanceEvents(@PathVariable String instanceId) {
        return new WebListResponse<>(runtimeReadFacade.instanceEvents(instanceId));
    }

    @PostMapping("/instance/{instanceId}/actions")
    public WebListResponse<WorkflowTaskAvailableAction> instanceAvailableActions(
            @PathVariable String instanceId,
            @RequestBody(required = false) WorkflowOperatorWebRequest request) {
        return new WebListResponse<>(runtimeReadFacade.instanceAvailableActions(instanceId,
                operatorIdOrNull(request == null ? null : request.operatorId())));
    }

    @PostMapping("/instance/{instanceId}/actions/{actionCode}")
    public WorkflowInstanceActionResult executeInstanceAction(
            @PathVariable String instanceId,
            @PathVariable String actionCode,
            @RequestBody(required = false) WorkflowInstanceActionWebRequest request) {
        return instanceActionFacade.execute(actionCode, new WorkflowInstanceActionRequest(instanceId,
                operatorId(request == null ? null : request.operatorId()),
                request == null ? null : request.reason(),
                null));
    }

    @PostMapping("/task/{taskId}/actions/{actionCode}")
    public WorkflowTaskActionResult executeTaskAction(
            @PathVariable String taskId,
            @PathVariable String actionCode,
            @RequestBody(required = false) WorkflowTaskActionWebRequest request) {
        return taskActionFacade.execute(actionCode, new WorkflowTaskActionRequest(taskId,
                operatorId(request == null ? null : request.operatorId()),
                request == null ? null : request.targetAssigneeId(),
                rejectResubmitMode(request == null ? null : request.rejectResubmitMode()),
                request == null ? null : request.reason(),
                null));
    }

    @PostMapping("/workbench/todo/query")
    public WebListResponse<WorkflowWorkbenchCard> todoCards(
            @RequestBody(required = false) WorkflowWorkbenchWebRequest request) {
        WorkflowWorkbenchWebRequest normalized = normalizeWorkbenchRequest(request);
        return new WebListResponse<>(runtimeReadFacade.todoCards(operatorId(normalized.operatorId()),
                page(normalized.page())));
    }

    @PostMapping("/workbench/done/query")
    public WebListResponse<WorkflowWorkbenchCard> doneCards(
            @RequestBody(required = false) WorkflowWorkbenchWebRequest request) {
        WorkflowWorkbenchWebRequest normalized = normalizeWorkbenchRequest(request);
        return new WebListResponse<>(runtimeReadFacade.doneCards(operatorId(normalized.operatorId()),
                page(normalized.page())));
    }

    @PostMapping("/workbench/notice/query")
    public WebListResponse<WorkflowWorkbenchCard> noticeCards(
            @RequestBody(required = false) WorkflowWorkbenchWebRequest request) {
        WorkflowWorkbenchWebRequest normalized = normalizeWorkbenchRequest(request);
        return new WebListResponse<>(runtimeReadFacade.noticeCards(operatorId(normalized.operatorId()),
                page(normalized.page())));
    }

    @PostMapping("/workbench/tracking/query")
    public WebListResponse<WorkflowWorkbenchCard> trackingCards(
            @RequestBody(required = false) WorkflowWorkbenchWebRequest request) {
        WorkflowWorkbenchWebRequest normalized = normalizeWorkbenchRequest(request);
        return new WebListResponse<>(runtimeReadFacade.trackingCards(operatorId(normalized.operatorId()),
                page(normalized.page())));
    }

    @GetMapping("/task/{taskId}/module-task/prepare")
    public WorkflowModuleTaskProcessBundle prepareModuleTask(@PathVariable String taskId) {
        return moduleTaskRuntimeService.prepare(taskId, operatorId(null));
    }

    @PostMapping("/task/{taskId}/module-task/check-and-continue")
    public WorkflowModuleTaskContinueResult checkAndContinueModuleTask(
            @PathVariable String taskId,
            @RequestBody(required = false) WorkflowModuleTaskContinueWebRequest request) {
        return moduleTaskRuntimeService.checkAndContinue(taskId,
                operatorId(request == null ? null : request.operatorId()),
                request == null ? null : request.reason());
    }

    @ExceptionHandler({IllegalArgumentException.class, PlatformException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public WorkflowWebError handleBadRequest(RuntimeException exception) {
        return new WorkflowWebError("bad_request", exception.getMessage());
    }

    private WorkflowWorkbenchWebRequest normalizeWorkbenchRequest(WorkflowWorkbenchWebRequest request) {
        return request == null ? new WorkflowWorkbenchWebRequest(null, WebPageRequest.DEFAULT) : request;
    }

    private PageRequest page(WebPageRequest request) {
        WebPageRequest normalized = request == null ? WebPageRequest.DEFAULT : request;
        return PageRequest.of(normalized.pageNum(), normalized.pageSize());
    }

    private String operatorIdOrNull(String operatorId) {
        return operatorId == null || operatorId.isBlank() ? null : operatorId;
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

    private WorkflowRejectResubmitMode rejectResubmitMode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (WorkflowRejectResubmitMode mode : WorkflowRejectResubmitMode.values()) {
            if (mode.name().equalsIgnoreCase(value) || mode.getCode().equals(value)) {
                return mode;
            }
        }
        throw new PlatformException("unsupported workflow reject resubmit mode: " + value);
    }
}

record WorkflowOperatorWebRequest(String operatorId) {
}

record WorkflowInstanceActionWebRequest(String operatorId, String reason) {
}

record WorkflowTaskActionWebRequest(String operatorId,
                                    String targetAssigneeId,
                                    String rejectResubmitMode,
                                    String reason) {
}

record WorkflowWorkbenchWebRequest(String operatorId, WebPageRequest page) {
}

record WorkflowModuleTaskContinueWebRequest(String operatorId, String reason) {
}

record WorkflowWebError(String code, String message) {
}
