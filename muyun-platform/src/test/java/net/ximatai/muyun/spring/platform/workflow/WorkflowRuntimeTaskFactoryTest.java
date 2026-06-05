package net.ximatai.muyun.spring.platform.workflow;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowRuntimeTaskFactoryTest {
    private final WorkflowRuntimeTaskFactory factory = new WorkflowRuntimeTaskFactory(new WorkflowRuntimeEventFactory());

    @Test
    void shouldCreateTasksForBlockingApprovalAndBusinessNodes() {
        WorkflowInstance instance = instance();
        WorkflowNodeInstance approval = node(instance, "approve");
        WorkflowNodeInstance businessTask = node(instance, "businessTask");
        WorkflowActivationResult activation = new WorkflowActivationResult(
                List.of("approve", "businessTask"),
                List.of(),
                List.of("approve"),
                List.of("businessTask"),
                List.of(),
                List.of(),
                false
        );

        WorkflowRuntimeTaskDraft draft = factory.createBlockingTasks(instance,
                List.of(approval, businessTask), activation, "user-1",
                Instant.parse("2026-06-05T01:00:00Z"));

        assertThat(draft.tasks()).hasSize(2);
        assertThat(draft.tasks()).extracting(WorkflowTask::getTaskKind)
                .containsExactly(WorkflowTaskKind.APPROVAL, WorkflowTaskKind.BUSINESS);
        assertThat(draft.tasks()).extracting(WorkflowTask::getAssigneeId)
                .containsExactly("user-1", "user-1");
        assertThat(draft.tasks().get(0).getCheckStatus()).isEqualTo(WorkflowTaskCheckStatus.NO_CHECK);
        assertThat(draft.tasks().get(1).getCheckStatus()).isEqualTo(WorkflowTaskCheckStatus.NOT_CHECKED);
        assertThat(draft.events()).hasSize(2)
                .allSatisfy(event -> assertThat(event.getEventType()).isEqualTo(WorkflowEventType.TASK_CREATED));
    }

    private WorkflowInstance instance() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId("instance-1");
        instance.setTenantId("tenant-1");
        return instance;
    }

    private WorkflowNodeInstance node(WorkflowInstance instance, String key) {
        WorkflowNodeInstance node = new WorkflowNodeInstance();
        node.setId(key + "-node");
        node.setInstanceId(instance.getId());
        node.setNodeKey(key);
        return node;
    }
}
