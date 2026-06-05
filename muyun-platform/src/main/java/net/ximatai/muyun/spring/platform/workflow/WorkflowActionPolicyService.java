package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import org.springframework.stereotype.Service;

@Service
public class WorkflowActionPolicyService {
    public void requireTaskOperator(WorkflowTask task, String actionCode, String operatorId) {
        requireText(actionCode, "workflow action code must not be blank");
        requireAssignee(task, operatorId);
    }

    public void requireNodeTaskAction(WorkflowTask task, WorkflowNodeInstance node, String actionCode,
                                      String operatorId, String reason) {
        requireText(actionCode, "workflow action code must not be blank");
        requireAssignee(task, operatorId);
        requireNodeAction(node, actionCode, reason);
    }

    public void requireInstanceAction(String actionCode, String reason) {
        requireText(actionCode, "workflow action code must not be blank");
        if ("revoke".equals(actionCode) || "terminate".equals(actionCode)) {
            requireReason(actionCode, reason);
        }
    }

    private void requireNodeAction(WorkflowNodeInstance node, String actionCode, String reason) {
        if (node == null) {
            throw new PlatformException("workflow node must not be null");
        }
        switch (actionCode) {
            case "reject" -> {
                if (Boolean.FALSE.equals(node.getAllowReject())) {
                    throw new PlatformException("workflow node does not allow reject: " + node.getNodeKey());
                }
                if (Boolean.TRUE.equals(node.getRequireRejectReason())) {
                    requireReason(actionCode, reason);
                }
            }
            case "rollback" -> {
                if (Boolean.FALSE.equals(node.getAllowRollback())) {
                    throw new PlatformException("workflow node does not allow rollback: " + node.getNodeKey());
                }
                if (Boolean.TRUE.equals(node.getRequireRollbackReason())) {
                    requireReason(actionCode, reason);
                }
            }
            case "terminate" -> {
                if (Boolean.FALSE.equals(node.getAllowTerminate())) {
                    throw new PlatformException("workflow node does not allow terminate: " + node.getNodeKey());
                }
                if (Boolean.TRUE.equals(node.getRequireTerminateReason())) {
                    requireReason(actionCode, reason);
                }
            }
            case "add_sign" -> {
                if (!Boolean.TRUE.equals(node.getAllowAddSign())) {
                    throw new PlatformException("workflow node does not allow add sign: " + node.getNodeKey());
                }
            }
            default -> {
            }
        }
    }

    public void requireRejectReturnToMe(WorkflowNodeInstance node) {
        if (node == null) {
            throw new PlatformException("workflow node must not be null");
        }
        if (!Boolean.TRUE.equals(node.getAllowRejectReturnToMe())) {
            throw new PlatformException("workflow node does not allow reject return to me: " + node.getNodeKey());
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

    private void requireReason(String actionCode, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new PlatformException("workflow action reason is required: " + actionCode);
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(message);
        }
        return value;
    }
}
