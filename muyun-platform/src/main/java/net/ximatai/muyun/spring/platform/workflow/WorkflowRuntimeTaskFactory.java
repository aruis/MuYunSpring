package net.ximatai.muyun.spring.platform.workflow;

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
            WorkflowTask task = task(instance, node, WorkflowTaskKind.APPROVAL, operatorId);
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
        task.setCheckStatus(taskKind == WorkflowTaskKind.BUSINESS
                ? WorkflowTaskCheckStatus.NOT_CHECKED : WorkflowTaskCheckStatus.NO_CHECK);
        return task;
    }
}
