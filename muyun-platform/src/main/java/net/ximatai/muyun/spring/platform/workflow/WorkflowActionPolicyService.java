package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.AllowAllActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class WorkflowActionPolicyService {
    private final ActionExecutionPolicyService executionPolicyService;

    public WorkflowActionPolicyService() {
        this(new AllowAllActionExecutionPolicyService());
    }

    public WorkflowActionPolicyService(ActionExecutionPolicyService executionPolicyService) {
        this.executionPolicyService = executionPolicyService == null
                ? new AllowAllActionExecutionPolicyService()
                : executionPolicyService;
    }

    public void requireRuntimeAction(WorkflowInstance instance, String actionCode) {
        if (instance == null) {
            throw new PlatformException("workflow instance must not be null");
        }
        String validActionCode = requireText(actionCode, "workflow action code must not be blank");
        String moduleAlias = requireText(instance.getModuleAlias(), "workflow module alias must not be blank");
        String recordId = requireText(instance.getRecordId(), "workflow record id must not be blank");
        executionPolicyService.requireRecordAction(ActionExecutionContext.ofPolicy(
                moduleAlias,
                runtimePolicy(validActionCode),
                Set.of(recordId),
                CurrentUserContext.currentUser()));
    }

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
        if ("revoke".equals(actionCode) || "terminate".equals(actionCode) || "reset".equals(actionCode)
                || "forceTerminate".equals(actionCode)) {
            requireReason(actionCode, reason);
        }
    }

    public void requireManagementTaskAction(String actionCode, String reason) {
        String validActionCode = requireText(actionCode, "workflow action code must not be blank");
        if ("forceApprove".equals(validActionCode) || "forceHandle".equals(validActionCode)) {
            requireReason(validActionCode, reason);
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
            case "addSign" -> {
                if (!Boolean.TRUE.equals(node.getAllowAddSign())) {
                    throw new PlatformException("workflow node does not allow add sign: " + node.getNodeKey());
                }
                requireReason(actionCode, reason);
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

    private ActionExecutionPolicy runtimePolicy(String actionCode) {
        return new ActionExecutionPolicy(
                actionCode,
                PlatformActionLevel.RECORD,
                ActionAccessMode.LOGIN_REQUIRED,
                false,
                false,
                ActionDefaultGrantPolicy.NONE,
                null
        );
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(message);
        }
        return value;
    }
}
