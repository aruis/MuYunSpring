package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.id.Ids;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkflowRuntimeTaskFactory {
    private final WorkflowRuntimeEventFactory eventFactory;

    public WorkflowRuntimeTaskFactory(WorkflowRuntimeEventFactory eventFactory) {
        this.eventFactory = eventFactory;
    }

    public WorkflowRuntimeTaskDraft createBlockingTasks(WorkflowInstance instance,
                                                        List<WorkflowNodeInstance> nodeInstances,
                                                        WorkflowActivationResult activation,
                                                        String operatorId,
                                                        Instant occurredAt) {
        Map<String, WorkflowNodeInstance> nodesByKey = nodeInstances.stream()
                .collect(Collectors.toMap(WorkflowNodeInstance::getNodeKey, Function.identity(), (left, right) -> left));
        List<WorkflowTask> tasks = new ArrayList<>();
        List<WorkflowEvent> events = new ArrayList<>();
        for (String nodeKey : activation.blockingApprovalNodeKeys()) {
            WorkflowNodeInstance node = nodesByKey.get(nodeKey);
            if (node == null) {
                continue;
            }
            WorkflowTask task = task(instance, node, WorkflowTaskKind.APPROVAL,
                    approvalAssignee(node, operatorId));
            tasks.add(task);
            events.add(eventFactory.taskCreated(instance, node, task, operatorId, occurredAt));
        }
        for (String nodeKey : activation.blockingTaskNodeKeys()) {
            WorkflowNodeInstance node = nodesByKey.get(nodeKey);
            if (node == null) {
                continue;
            }
            WorkflowTask task = task(instance, node, WorkflowTaskKind.BUSINESS, operatorId);
            tasks.add(task);
            events.add(eventFactory.taskCreated(instance, node, task, operatorId, occurredAt));
        }
        return new WorkflowRuntimeTaskDraft(tasks, events);
    }

    private WorkflowTask task(WorkflowInstance instance, WorkflowNodeInstance node,
                              WorkflowTaskKind taskKind, String ownerId) {
        WorkflowTask task = new WorkflowTask();
        task.setId(Ids.newId());
        task.setTenantId(instance.getTenantId());
        task.setInstanceId(instance.getId());
        task.setNodeInstanceId(node.getId());
        task.setTaskKind(taskKind);
        task.setTaskStatus(WorkflowTaskStatus.TODO);
        task.setOwnerId(ownerId);
        task.setOriginalAssigneeId(ownerId);
        task.setAssigneeId(ownerId);
        task.setAssignmentKind(WorkflowAssignmentKind.NORMAL);
        task.setAssignmentPolicyText(node.getParticipantPolicyText());
        task.setAssignmentSnapshotText(ownerId == null ? null : "{\"assigneeId\":\"" + escape(ownerId) + "\"}");
        task.setCheckStatus(taskKind == WorkflowTaskKind.BUSINESS
                ? WorkflowTaskCheckStatus.NOT_CHECKED : WorkflowTaskCheckStatus.NO_CHECK);
        return task;
    }

    private String approvalAssignee(WorkflowNodeInstance node, String operatorId) {
        String policy = node.getParticipantPolicyText();
        if (policy == null || policy.isBlank()) {
            if (Boolean.TRUE.equals(node.getAddedByAddSign())) {
                throw new PlatformException("workflow add sign node participant policy is required: "
                        + node.getNodeKey());
            }
            return operatorId;
        }
        String trimmed = policy.trim();
        if (!trimmed.startsWith("user:")) {
            throw new PlatformException("workflow participant policy only supports user:<userId>: "
                    + node.getNodeKey());
        }
        String userId = trimmed.substring("user:".length()).trim();
        if (userId.isBlank()) {
            throw new PlatformException("workflow participant policy user id must not be blank: "
                    + node.getNodeKey());
        }
        if (userId.indexOf(',') >= 0 || userId.indexOf(';') >= 0 || userId.contains("[") || userId.contains("]")) {
            throw new PlatformException("workflow participant policy only supports single user in first version: "
                    + node.getNodeKey());
        }
        return userId;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
