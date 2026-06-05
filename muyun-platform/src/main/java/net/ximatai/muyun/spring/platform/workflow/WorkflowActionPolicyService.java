package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class WorkflowActionPolicyService {
    private static final Set<String> REASON_REQUIRED_ACTIONS = Set.of(
            "reject",
            "rollback",
            "transfer",
            "invalidate",
            "cancel",
            "revoke",
            "terminate"
    );

    public void requireTaskOperator(WorkflowTask task, String actionCode, String operatorId) {
        requireReasonIfNeeded(actionCode, null);
        requireAssignee(task, operatorId);
    }

    public void requireTaskOperator(WorkflowTask task, String actionCode, String operatorId, String reason) {
        requireReasonIfNeeded(actionCode, reason);
        requireAssignee(task, operatorId);
    }

    public void requireInstanceAction(String actionCode, String reason) {
        requireReasonIfNeeded(actionCode, reason);
    }

    private void requireReasonIfNeeded(String actionCode, String reason) {
        if (REASON_REQUIRED_ACTIONS.contains(requireText(actionCode, "workflow action code must not be blank"))
                && (reason == null || reason.isBlank())) {
            throw new PlatformException("workflow action reason is required: " + actionCode);
        }
    }

    private void requireAssignee(WorkflowTask task, String operatorId) {
        if (task == null) {
            throw new PlatformException("workflow task must not be null");
        }
        String validOperatorId = requireText(operatorId, "workflow operator id must not be blank");
        if (CurrentUserContext.currentUser()
                .filter(user -> user.system() && validOperatorId.equals(user.userId()))
                .isPresent()) {
            return;
        }
        String assigneeId = requireText(task.getAssigneeId(), "workflow task assignee id must not be blank");
        if (!validOperatorId.equals(assigneeId)) {
            throw new PlatformException("workflow task action operator is not assignee: " + task.getId());
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(message);
        }
        return value;
    }
}
