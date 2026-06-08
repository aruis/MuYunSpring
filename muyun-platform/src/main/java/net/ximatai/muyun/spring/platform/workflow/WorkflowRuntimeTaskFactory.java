package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.id.Ids;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final WorkflowDelegationService delegationService;

    public WorkflowRuntimeTaskFactory(WorkflowRuntimeEventFactory eventFactory) {
        this(eventFactory, null);
    }

    @Autowired
    public WorkflowRuntimeTaskFactory(WorkflowRuntimeEventFactory eventFactory,
                                      WorkflowDelegationService delegationService) {
        this.eventFactory = eventFactory;
        this.delegationService = delegationService;
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
        applyDelegation(instance, task);
        return task;
    }

    private void applyDelegation(WorkflowInstance instance, WorkflowTask task) {
        if (delegationService == null || task.getTaskKind() == WorkflowTaskKind.NOTICE
                || task.getTaskKind() == WorkflowTaskKind.RESUBMIT) {
            return;
        }
        WorkflowDelegationMatch match = delegationService.match(task.getOriginalAssigneeId(),
                instance.getModuleAlias(), instance.getAuthOrgId());
        if (match == null) {
            return;
        }
        task.setAssignmentKind(WorkflowAssignmentKind.DELEGATED);
        task.setAssigneeId(match.delegateUserId());
        task.setDelegatedFromUserId(match.principalUserId());
        task.setDelegatedToUserId(match.delegateUserId());
        task.setPrincipalCanProcess(match.principalCanProcess());
        task.setDelegationPolicyId(match.delegationPolicyId());
        task.setAssignmentSnapshotText(match.snapshotText());
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
        return WorkflowParticipantPolicyCodec.parse(policy, node.getNodeKey())
                .requireSingleUser(
                        "workflow participant policy user id must not be blank: " + node.getNodeKey(),
                        "workflow participant policy only supports single user in first version: "
                                + node.getNodeKey());
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
