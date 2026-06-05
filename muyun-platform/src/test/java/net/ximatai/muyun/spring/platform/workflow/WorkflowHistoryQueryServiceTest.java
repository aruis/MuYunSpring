package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowHistoryQueryServiceTest {
    private final WorkflowHistoryInstanceDao historyDao = mock(WorkflowHistoryInstanceDao.class);
    private final WorkflowArchiveService archiveService = mock(WorkflowArchiveService.class);
    private final WorkflowHistoryQueryService service = new WorkflowHistoryQueryService(historyDao, archiveService);

    @Test
    void shouldQueryRecordHistoryByModuleAndRecord() {
        when(historyDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of());

        service.queryRecordHistory("sales.contract", "record-1", PageRequest.of(1, 20));

        verify(historyDao).query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class));
    }

    @Test
    void shouldRejectMissingHistoryInstance() {
        assertThatThrownBy(() -> service.renderBundle("missing"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("workflow history instance not found");
    }

    @Test
    void shouldExposeHistoryTaskExplanationFieldsFromSnapshot() {
        WorkflowHistoryInstance history = history("history-1");
        WorkflowTask task = task("task-1", WorkflowTaskStatus.CANCELED);
        when(historyDao.findById("history-1")).thenReturn(history);
        when(archiveService.parseSnapshot(history)).thenReturn(snapshot(task, List.of()));

        List<WorkflowHistoryTaskView> views = service.taskViews("history-1");

        assertThat(views).hasSize(1);
        assertThat(views.getFirst().actualProcessUserId()).isEqualTo("delegate-1");
        assertThat(views.getFirst().processedByDelegation()).isTrue();
        assertThat(views.getFirst().assignmentKind()).isEqualTo(WorkflowAssignmentKind.DELEGATED);
        assertThat(views.getFirst().originalAssigneeId()).isEqualTo("principal-1");
        assertThat(views.getFirst().delegatedFromUserId()).isEqualTo("principal-1");
        assertThat(views.getFirst().delegatedToUserId()).isEqualTo("delegate-1");
        assertThat(views.getFirst().principalCanProcess()).isTrue();
        assertThat(views.getFirst().delegationPolicyId()).isEqualTo("delegation-1");
        assertThat(views.getFirst().delegationSnapshot()).contains("delegate-1");
        assertThat(views.getFirst().canceled()).isTrue();
        assertThat(views.getFirst().invalidated()).isFalse();
    }

    @Test
    void shouldExposeHistoryEventExplanationFieldsFromRelatedTask() {
        WorkflowHistoryInstance history = history("history-1");
        WorkflowTask task = task("task-1", WorkflowTaskStatus.INVALIDATED);
        WorkflowEvent event = new WorkflowEvent();
        event.setId("event-1");
        event.setInstanceId("instance-1");
        event.setTaskId("task-1");
        event.setEventType(WorkflowEventType.TASK_INVALIDATED);
        event.setActionCode("invalidate");
        event.setOperatorId("delegate-1");
        event.setOccurredAt(Instant.parse("2026-06-05T03:00:00Z"));
        when(historyDao.findById("history-1")).thenReturn(history);
        when(archiveService.parseSnapshot(history)).thenReturn(snapshot(task, List.of(event)));

        List<WorkflowHistoryEventView> views = service.eventViews("history-1");

        assertThat(views).hasSize(1);
        assertThat(views.getFirst().actualProcessUserId()).isEqualTo("delegate-1");
        assertThat(views.getFirst().processedByDelegation()).isTrue();
        assertThat(views.getFirst().assignmentKind()).isEqualTo(WorkflowAssignmentKind.DELEGATED);
        assertThat(views.getFirst().delegationPolicyId()).isEqualTo("delegation-1");
        assertThat(views.getFirst().taskInvalidated()).isTrue();
        assertThat(views.getFirst().taskCanceled()).isFalse();
    }

    private WorkflowHistoryInstance history(String id) {
        WorkflowHistoryInstance history = new WorkflowHistoryInstance();
        history.setId(id);
        return history;
    }

    private WorkflowTask task(String id, WorkflowTaskStatus status) {
        WorkflowTask task = new WorkflowTask();
        task.setId(id);
        task.setInstanceId("instance-1");
        task.setNodeInstanceId("node-1");
        task.setTaskKind(WorkflowTaskKind.APPROVAL);
        task.setTaskStatus(status);
        task.setAssignmentKind(WorkflowAssignmentKind.DELEGATED);
        task.setOriginalAssigneeId("principal-1");
        task.setAssigneeId("delegate-1");
        task.setActualProcessorId("delegate-1");
        task.setDelegatedFromUserId("principal-1");
        task.setDelegatedToUserId("delegate-1");
        task.setPrincipalCanProcess(true);
        task.setDelegationPolicyId("delegation-1");
        task.setAssignmentSnapshotText("{\"delegateUserId\":\"delegate-1\"}");
        task.setDecision("cancel");
        task.setCreatedAt(Instant.parse("2026-06-05T01:00:00Z"));
        task.setCompletedAt(Instant.parse("2026-06-05T02:00:00Z"));
        return task;
    }

    private WorkflowHistorySnapshot snapshot(WorkflowTask task, List<WorkflowEvent> events) {
        return new WorkflowHistorySnapshot(1, Instant.parse("2026-06-05T04:00:00Z"),
                WorkflowArchiveReason.RECALLED, new WorkflowInstance(), List.of(), List.of(), List.of(task), events);
    }
}
