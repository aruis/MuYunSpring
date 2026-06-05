package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.stereotype.Service;

@Service
public class WorkflowInstanceActionFacade {
    private final WorkflowInstanceActionService actionService;

    public WorkflowInstanceActionFacade(WorkflowInstanceActionService actionService) {
        this.actionService = actionService;
    }

    public WorkflowInstanceActionResult execute(String actionCode, WorkflowInstanceActionRequest request) {
        return switch (requireActionCode(actionCode)) {
            case "revoke" -> actionService.revoke(request);
            case "reset" -> actionService.reset(request);
            case "terminate" -> actionService.terminate(request);
            case "forceTerminate" -> actionService.forceTerminate(request);
            default -> throw new PlatformException("unsupported workflow instance action: " + actionCode);
        };
    }

    private String requireActionCode(String actionCode) {
        if (actionCode == null || actionCode.isBlank()) {
            throw new PlatformException("workflow action code must not be blank");
        }
        return actionCode;
    }
}
