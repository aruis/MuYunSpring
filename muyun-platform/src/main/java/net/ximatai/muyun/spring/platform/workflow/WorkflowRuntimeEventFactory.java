package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.id.Ids;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class WorkflowRuntimeEventFactory {
    public WorkflowEvent instanceStarted(WorkflowInstance instance, String operatorId, Instant occurredAt) {
        return event(instance, null, null, WorkflowEventType.INSTANCE_STARTED, "submit", operatorId,
                "workflow instance started", null, occurredAt);
    }

    public WorkflowEvent nodeActivated(WorkflowInstance instance, WorkflowNodeInstance node,
                                       String operatorId, Instant occurredAt) {
        return event(instance, node, null, WorkflowEventType.NODE_ACTIVATED, null, operatorId,
                "workflow node activated", null, occurredAt);
    }

    public WorkflowEvent taskCreated(WorkflowInstance instance, WorkflowNodeInstance node, WorkflowTask task,
                                     String operatorId, Instant occurredAt) {
        return event(instance, node, task, WorkflowEventType.TASK_CREATED, null, operatorId,
                "workflow task created", null, occurredAt);
    }

    public WorkflowEvent taskCreated(WorkflowInstance instance, WorkflowTask task,
                                     String operatorId, Instant occurredAt) {
        return event(instance, null, task, WorkflowEventType.TASK_CREATED, null, operatorId,
                "workflow task created", null, occurredAt);
    }

    public WorkflowEvent taskCompleted(WorkflowInstance instance, WorkflowTask task, String actionCode,
                                       String operatorId, String message, Instant occurredAt) {
        return event(instance, null, task, WorkflowEventType.TASK_COMPLETED, actionCode, operatorId,
                message == null || message.isBlank() ? "workflow task completed" : message, null, occurredAt);
    }

    public WorkflowEvent taskTransferred(WorkflowInstance instance, WorkflowTask task, String operatorId,
                                         String message, String payloadText, Instant occurredAt) {
        return event(instance, null, task, WorkflowEventType.TASK_TRANSFERRED, "transfer", operatorId,
                message, payloadText, occurredAt);
    }

    public WorkflowEvent addSign(WorkflowInstance instance, WorkflowNodeInstance node, WorkflowTask task,
                                 String operatorId, String message, String payloadText, Instant occurredAt) {
        return event(instance, node, task, WorkflowEventType.ADD_SIGN, "addSign", operatorId,
                message == null || message.isBlank() ? "workflow runtime add sign" : message,
                payloadText, occurredAt);
    }

    public WorkflowEvent taskRejected(WorkflowInstance instance, WorkflowTask task,
                                      String operatorId, String message, Instant occurredAt) {
        return event(instance, null, task, WorkflowEventType.TASK_REJECTED, "reject", operatorId,
                message == null || message.isBlank() ? "workflow task rejected" : message, null, occurredAt);
    }

    public WorkflowEvent taskResubmitted(WorkflowInstance instance, WorkflowTask task,
                                         String operatorId, String message, Instant occurredAt) {
        return event(instance, null, task, WorkflowEventType.TASK_RESUBMITTED, "resubmit", operatorId,
                message == null || message.isBlank() ? "workflow task resubmitted" : message, null, occurredAt);
    }

    public WorkflowEvent nodeRolledBack(WorkflowInstance instance, WorkflowNodeInstance node,
                                        String operatorId, String message, Instant occurredAt) {
        return event(instance, node, null, WorkflowEventType.NODE_ROLLED_BACK, "rollback", operatorId,
                message == null || message.isBlank() ? "workflow node rolled back" : message, null, occurredAt);
    }

    public WorkflowEvent overtimeWarned(WorkflowInstance instance, WorkflowNodeInstance node,
                                        String operatorId, String payloadText, Instant occurredAt) {
        return event(instance, node, null, WorkflowEventType.OVERTIME_WARNED, "overtime_warn", operatorId,
                "workflow node overtime warned", payloadText, occurredAt);
    }

    public WorkflowEvent overtimeOverdue(WorkflowInstance instance, WorkflowNodeInstance node,
                                         String operatorId, String payloadText, Instant occurredAt) {
        return event(instance, node, null, WorkflowEventType.OVERTIME_OVERDUE, "overtime_overdue", operatorId,
                "workflow node overdue", payloadText, occurredAt);
    }

    public WorkflowEvent taskSkipped(WorkflowInstance instance, WorkflowTask task,
                                     String operatorId, String message, Instant occurredAt) {
        return event(instance, null, task, WorkflowEventType.TASK_SKIPPED, "skip", operatorId,
                message == null || message.isBlank() ? "workflow task skipped" : message, null, occurredAt);
    }

    public WorkflowEvent taskInvalidated(WorkflowInstance instance, WorkflowTask task,
                                         String operatorId, String message, Instant occurredAt) {
        return event(instance, null, task, WorkflowEventType.TASK_INVALIDATED, "invalidate", operatorId,
                message == null || message.isBlank() ? "workflow task invalidated" : message, null, occurredAt);
    }

    public WorkflowEvent taskCanceled(WorkflowInstance instance, WorkflowTask task,
                                      String operatorId, String message, Instant occurredAt) {
        return event(instance, null, task, WorkflowEventType.TASK_CANCELED, "cancel", operatorId,
                message == null || message.isBlank() ? "workflow task canceled" : message, null, occurredAt);
    }

    public WorkflowEvent routeSelected(WorkflowInstance instance, WorkflowRouteInstance route,
                                       String operatorId, Instant occurredAt) {
        return event(instance, null, null, WorkflowEventType.ROUTE_SELECTED, "route", operatorId,
                "workflow route selected: " + route.getRouteKey(), null, occurredAt);
    }

    public WorkflowEvent routeDropped(WorkflowInstance instance, WorkflowRouteInstance route,
                                      String operatorId, Instant occurredAt) {
        return event(instance, null, null, WorkflowEventType.ROUTE_DROPPED, "route", operatorId,
                "workflow route dropped: " + route.getRouteKey(), route.getClosedReason(), occurredAt);
    }

    public WorkflowEvent approvalCompleted(WorkflowInstance instance, String operatorId, Instant occurredAt) {
        return event(instance, null, null, WorkflowEventType.APPROVAL_COMPLETED, "approval_completed", operatorId,
                "workflow approval completed", null, occurredAt);
    }

    public WorkflowEvent instanceCompleted(WorkflowInstance instance, String operatorId, Instant occurredAt) {
        return event(instance, null, null, WorkflowEventType.INSTANCE_COMPLETED, "complete", operatorId,
                "workflow instance completed", null, occurredAt);
    }

    public WorkflowEvent instanceRevoked(WorkflowInstance instance, String operatorId,
                                         String message, Instant occurredAt) {
        return event(instance, null, null, WorkflowEventType.INSTANCE_REVOKED, "revoke", operatorId,
                message == null || message.isBlank() ? "workflow instance revoked" : message, null, occurredAt);
    }

    public WorkflowEvent instanceReset(WorkflowInstance instance, String operatorId,
                                       String message, Instant occurredAt) {
        return event(instance, null, null, WorkflowEventType.INSTANCE_RESET, "reset", operatorId,
                message == null || message.isBlank() ? "workflow instance reset" : message, null, occurredAt);
    }

    public WorkflowEvent instanceTerminated(WorkflowInstance instance, String operatorId,
                                            String message, Instant occurredAt) {
        return event(instance, null, null, WorkflowEventType.INSTANCE_TERMINATED, "terminate", operatorId,
                message == null || message.isBlank() ? "workflow instance terminated" : message, null, occurredAt);
    }

    public WorkflowEvent instanceForceTerminated(WorkflowInstance instance, String operatorId,
                                                 String message, Instant occurredAt) {
        return event(instance, null, null, WorkflowEventType.INSTANCE_TERMINATED, "forceTerminate", operatorId,
                message == null || message.isBlank() ? "workflow instance force terminated" : message, null, occurredAt);
    }

    private WorkflowEvent event(WorkflowInstance instance, WorkflowNodeInstance node, WorkflowTask task,
                                WorkflowEventType eventType, String actionCode, String operatorId,
                                String message, String payloadText, Instant occurredAt) {
        WorkflowEvent event = new WorkflowEvent();
        event.setId(Ids.newId());
        event.setTenantId(instance.getTenantId());
        event.setInstanceId(instance.getId());
        event.setNodeInstanceId(node == null ? null : node.getId());
        event.setTaskId(task == null ? null : task.getId());
        event.setEventType(eventType);
        event.setActionCode(actionCode);
        event.setOperatorId(operatorId);
        event.setMessage(message);
        event.setPayloadText(payloadText);
        event.setOccurredAt(occurredAt == null ? Instant.now() : occurredAt);
        return event;
    }
}
