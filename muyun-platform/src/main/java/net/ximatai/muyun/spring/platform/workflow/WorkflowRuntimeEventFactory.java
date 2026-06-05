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
