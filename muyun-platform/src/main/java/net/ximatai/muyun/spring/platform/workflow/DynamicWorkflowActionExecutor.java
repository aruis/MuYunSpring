package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionResultBody;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class DynamicWorkflowActionExecutor implements DynamicActionExecutor {
    public static final String EXECUTOR_KEY = "platform.workflow";

    private static final String ACTION_SUBMIT_APPROVAL = "submitApproval";
    private static final String ACTION_SUBMIT_WORKFLOW = "submitWorkflow";
    private static final String ACTION_TASK_ACTION = "taskAction";
    private static final String ACTION_AVAILABLE_TASK_ACTIONS = "availableTaskActions";

    private final WorkflowModuleSubmitService submitService;
    private final WorkflowTaskActionFacade taskActionFacade;

    public DynamicWorkflowActionExecutor(WorkflowModuleSubmitService submitService,
                                         WorkflowTaskActionFacade taskActionFacade) {
        this.submitService = submitService;
        this.taskActionFacade = taskActionFacade;
    }

    @Override
    public String executorKey() {
        return EXECUTOR_KEY;
    }

    @Override
    public Object execute(DynamicActionExecutionContext context, DynamicActionExecutionRequest request) {
        String workflowAction = text(payload(request, "workflowAction"), context == null ? null : context.actionCode());
        return switch (workflowAction) {
            case ACTION_SUBMIT_APPROVAL -> DynamicActionResultBody.refreshed(
                    submitService.submitApproval(moduleAlias(context), recordId(context, request)));
            case ACTION_SUBMIT_WORKFLOW -> DynamicActionResultBody.refreshed(
                    submitService.submitWorkflow(moduleAlias(context), recordId(context, request),
                            requireText(payload(request, "definitionAlias"), "workflow definition alias must not be blank")));
            case ACTION_TASK_ACTION -> DynamicActionResultBody.refreshed(taskActionFacade.execute(
                    requireText(payload(request, "taskActionCode"), "workflow task action code must not be blank"),
                    taskRequest(request)));
            case ACTION_AVAILABLE_TASK_ACTIONS -> DynamicActionResultBody.of(taskActionFacade.availableActions(
                    requireText(payload(request, "taskId"), "workflow task id must not be blank"),
                    operatorId(context, request)));
            default -> throw new PlatformException("unsupported dynamic workflow action: " + workflowAction);
        };
    }

    private WorkflowTaskActionRequest taskRequest(DynamicActionExecutionRequest request) {
        return new WorkflowTaskActionRequest(
                requireText(payload(request, "taskId"), "workflow task id must not be blank"),
                text(payload(request, "operatorId"), null),
                text(payload(request, "targetAssigneeId"), null),
                rejectResubmitMode(payload(request, "rejectResubmitMode")),
                text(payload(request, "reason"), null),
                operatedAt(payload(request, "operatedAt"))
        );
    }

    private WorkflowRejectResubmitMode rejectResubmitMode(Object value) {
        String mode = text(value, null);
        if (mode == null) {
            return null;
        }
        for (WorkflowRejectResubmitMode candidate : WorkflowRejectResubmitMode.values()) {
            if (candidate.name().equalsIgnoreCase(mode) || candidate.getCode().equalsIgnoreCase(mode)) {
                return candidate;
            }
        }
        throw new PlatformException("unsupported workflow reject resubmit mode: " + mode);
    }

    private Instant operatedAt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        return Instant.parse(requireText(value, "workflow operatedAt must not be blank"));
    }

    private String moduleAlias(DynamicActionExecutionContext context) {
        return requireText(context == null ? null : context.moduleAlias(), "workflow module alias must not be blank");
    }

    private String recordId(DynamicActionExecutionContext context, DynamicActionExecutionRequest request) {
        String recordId = text(request == null ? null : request.recordId(), null);
        if (recordId == null && request != null && request.record() != null) {
            recordId = text(request.record().getId(), null);
        }
        if (recordId == null && context != null) {
            recordId = text(context.recordId(), null);
        }
        return requireText(recordId, "workflow record id must not be blank");
    }

    private String operatorId(DynamicActionExecutionContext context, DynamicActionExecutionRequest request) {
        String operatorId = text(payload(request, "operatorId"), null);
        if (operatorId != null) {
            return operatorId;
        }
        if (context != null && text(context.operatorId(), null) != null) {
            return context.operatorId();
        }
        return CurrentUserContext.currentUser()
                .map(user -> user.userId())
                .orElse("system");
    }

    private Object payload(DynamicActionExecutionRequest request, String key) {
        Map<String, Object> payload = request == null ? Map.of() : request.payload();
        return payload.get(key);
    }

    private String requireText(Object value, String message) {
        String text = text(value, null);
        if (text == null) {
            throw new PlatformException(message);
        }
        return text;
    }

    private String text(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? defaultValue : text;
    }
}
