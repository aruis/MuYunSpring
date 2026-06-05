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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WorkflowRuntimeReadFacadeTest {
    private final WorkflowInstanceDao instanceDao = mock(WorkflowInstanceDao.class);
    private final WorkflowTaskDao taskDao = mock(WorkflowTaskDao.class);
    private final WorkflowNodeInstanceDao nodeDao = mock(WorkflowNodeInstanceDao.class);
    private final WorkflowRouteInstanceDao routeDao = mock(WorkflowRouteInstanceDao.class);
    private final WorkflowEventDao eventDao = mock(WorkflowEventDao.class);
    private final WorkflowTaskActionAvailabilityService availabilityService =
            mock(WorkflowTaskActionAvailabilityService.class);
    private final WorkflowActionPolicyService actionPolicyService = mock(WorkflowActionPolicyService.class);
    private final WorkflowRuntimeReadFacade facade = new WorkflowRuntimeReadFacade(
            instanceDao, taskDao, nodeDao, routeDao, eventDao, availabilityService, actionPolicyService);

    @Test
    void shouldLoadRuntimeRenderBundle() {
        WorkflowInstance instance = instance("instance-1");
        WorkflowNodeInstance node = node("node-1", "approve");
        WorkflowRouteInstance route = route();
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(nodeDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class))).thenReturn(List.of(node));
        when(routeDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class))).thenReturn(List.of(route));

        WorkflowRuntimeRenderBundle bundle = facade.renderBundle("instance-1");

        assertThat(bundle.mode()).isEqualTo("RUNTIME");
        assertThat(bundle.instance()).isSameAs(instance);
        assertThat(bundle.nodes()).containsExactly(node);
        assertThat(bundle.routes()).containsExactly(route);
        verify(actionPolicyService).requireRecordView(instance);
    }

    @Test
    void shouldLoadInstanceAvailableActionsWithTaskAndNodeContext() {
        WorkflowInstance instance = instance("instance-1");
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        WorkflowNodeInstance node = node("node-1", "approve");
        WorkflowTaskAvailableAction reject = WorkflowTaskAvailableAction.of("reject", "驳回")
                .supportRejectReturnToMe(true);
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(nodeDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class))).thenReturn(List.of(node));
        when(taskDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class))).thenReturn(List.of(task));
        when(availabilityService.availableActions("task-1", "user-1")).thenReturn(List.of(reject));

        List<WorkflowTaskAvailableAction> actions = facade.instanceAvailableActions("instance-1", "user-1");

        assertThat(actions).hasSize(1);
        assertThat(actions.getFirst().taskId()).isEqualTo("task-1");
        assertThat(actions.getFirst().nodeKey()).isEqualTo("approve");
        assertThat(actions.getFirst().rejectResubmitModes()).containsExactly("restart", "return_to_me");
        assertThat(actions.getFirst().defaultRejectResubmitMode()).isEqualTo("restart");
        verify(actionPolicyService).requireRecordView(instance);
    }

    @Test
    void shouldNotExposeActionWhenOperatorIsNotTaskAssignee() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        task.setAssigneeId("user-2");
        task.setDelegatedFromUserId("user-1");
        when(instanceDao.findById("instance-1")).thenReturn(instance("instance-1"));
        when(nodeDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class))).thenReturn(List.of());
        when(taskDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class))).thenReturn(List.of(task));

        assertThat(facade.instanceAvailableActions("instance-1", "user-1")).isEmpty();
        verifyNoInteractions(availabilityService);
    }

    @Test
    void shouldBuildTodoWorkbenchCardsFromTasks() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.BUSINESS, WorkflowTaskStatus.TODO);
        task.setCreatedAt(Instant.parse("2026-06-05T01:00:00Z"));
        WorkflowInstance instance = instance("instance-1");
        WorkflowNodeInstance node = node("node-1", "visit");
        node.setOvertimeStatus(WorkflowOvertimeStatus.WARNED);
        when(taskDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of(task));
        when(instanceDao.findById("instance-1")).thenReturn(instance);
        when(nodeDao.findById("node-1")).thenReturn(node);

        List<WorkflowWorkbenchCard> cards = facade.todoCards("user-1", PageRequest.of(1, 20));

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().boardType()).isEqualTo("TODO");
        assertThat(cards.getFirst().instanceId()).isEqualTo("instance-1");
        assertThat(cards.getFirst().taskId()).isEqualTo("task-1");
        assertThat(cards.getFirst().nodeKey()).isEqualTo("visit");
        assertThat(cards.getFirst().currentAssigneeIds()).containsExactly("user-1");
        assertThat(cards.getFirst().overtimeStatus()).isEqualTo(WorkflowOvertimeStatus.WARNED);
    }

    @Test
    void shouldFilterWorkbenchCardsByStableRequestFields() {
        WorkflowTask matching = task("task-1", WorkflowTaskKind.BUSINESS, WorkflowTaskStatus.TODO);
        matching.setCreatedAt(Instant.parse("2026-06-05T01:00:00Z"));
        WorkflowTask skipped = task("task-2", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        skipped.setInstanceId("instance-2");
        skipped.setNodeInstanceId("node-2");
        skipped.setCreatedAt(Instant.parse("2026-06-05T02:00:00Z"));
        WorkflowInstance matchingInstance = instance("instance-1");
        matchingInstance.setDefinitionId("definition-1");
        matchingInstance.setWorkflowVersionId("version-1");
        matchingInstance.setStartedAt(Instant.parse("2026-06-05T00:00:00Z"));
        WorkflowInstance skippedInstance = instance("instance-2");
        skippedInstance.setModuleAlias("sales.order");
        skippedInstance.setRecordId("record-2");
        skippedInstance.setDefinitionId("definition-2");
        skippedInstance.setWorkflowVersionId("version-2");
        WorkflowNodeInstance matchingNode = node("node-1", "visit");
        matchingNode.setOvertimeStatus(WorkflowOvertimeStatus.WARNED);
        WorkflowNodeInstance skippedNode = node("node-2", "approve");
        skippedNode.setOvertimeStatus(WorkflowOvertimeStatus.NORMAL);
        when(taskDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of(matching, skipped));
        when(instanceDao.findById("instance-1")).thenReturn(matchingInstance);
        when(instanceDao.findById("instance-2")).thenReturn(skippedInstance);
        when(nodeDao.findById("node-1")).thenReturn(matchingNode);
        when(nodeDao.findById("node-2")).thenReturn(skippedNode);
        WorkflowWorkbenchQueryRequest request = new WorkflowWorkbenchQueryRequest("sales.contract", "record-1",
                "definition-1", "version-1", null, WorkflowInstanceStatus.RUNNING, "visit",
                WorkflowTaskKind.BUSINESS, WorkflowTaskStatus.TODO, WorkflowAssignmentKind.NORMAL,
                WorkflowOvertimeStatus.WARNED, Instant.parse("2026-06-04T00:00:00Z"),
                Instant.parse("2026-06-06T00:00:00Z"), Instant.parse("2026-06-05T00:00:00Z"),
                Instant.parse("2026-06-05T02:00:00Z"), null, null, null, null, null, null, List.of());

        List<WorkflowWorkbenchCard> cards = facade.todoCards("user-1", PageRequest.of(1, 20), request);

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().taskId()).isEqualTo("task-1");
        assertThat(cards.getFirst().definitionId()).isEqualTo("definition-1");
        assertThat(cards.getFirst().workflowVersionId()).isEqualTo("version-1");
        assertThat(cards.getFirst().startedAt()).isEqualTo(Instant.parse("2026-06-05T00:00:00Z"));
    }

    @Test
    void shouldSortWorkbenchCardsByWhitelistedField() {
        WorkflowTask earlier = task("task-1", WorkflowTaskKind.BUSINESS, WorkflowTaskStatus.TODO);
        earlier.setCreatedAt(Instant.parse("2026-06-05T01:00:00Z"));
        WorkflowTask later = task("task-2", WorkflowTaskKind.BUSINESS, WorkflowTaskStatus.TODO);
        later.setCreatedAt(Instant.parse("2026-06-05T03:00:00Z"));
        when(taskDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of(later, earlier));
        when(instanceDao.findById("instance-1")).thenReturn(instance("instance-1"));
        when(nodeDao.findById("node-1")).thenReturn(node("node-1", "visit"));
        WorkflowWorkbenchQueryRequest request = new WorkflowWorkbenchQueryRequest(null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                List.of(new WorkflowWorkbenchSort("receivedAt", WorkflowSortDirection.ASC)));

        List<WorkflowWorkbenchCard> cards = facade.todoCards("user-1", PageRequest.of(1, 20), request);

        assertThat(cards).extracting(WorkflowWorkbenchCard::taskId).containsExactly("task-1", "task-2");
    }

    @Test
    void shouldRejectUnsupportedWorkbenchSortField() {
        when(taskDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of());
        WorkflowWorkbenchQueryRequest request = new WorkflowWorkbenchQueryRequest(null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                List.of(new WorkflowWorkbenchSort("rawSql", WorkflowSortDirection.ASC)));

        assertThatThrownBy(() -> facade.todoCards("user-1", PageRequest.of(1, 20), request))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("unsupported workflow workbench sort field");
    }

    @Test
    void shouldBuildTodoWorkbenchCardForPrincipalCanProcessDelegatedTask() {
        WorkflowTask task = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        task.setAssignmentKind(WorkflowAssignmentKind.DELEGATED);
        task.setOriginalAssigneeId("principal-1");
        task.setAssigneeId("delegate-1");
        task.setDelegatedFromUserId("principal-1");
        task.setDelegatedToUserId("delegate-1");
        task.setPrincipalCanProcess(true);
        task.setCreatedAt(Instant.parse("2026-06-05T01:00:00Z"));
        when(taskDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of(task));
        when(instanceDao.findById("instance-1")).thenReturn(instance("instance-1"));
        when(nodeDao.findById("node-1")).thenReturn(node("node-1", "approve"));

        List<WorkflowWorkbenchCard> cards = facade.todoCards("principal-1", PageRequest.of(1, 20));

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().assignmentKind()).isEqualTo(WorkflowAssignmentKind.DELEGATED);
        assertThat(cards.getFirst().originalAssigneeId()).isEqualTo("principal-1");
        assertThat(cards.getFirst().delegatedFromUserId()).isEqualTo("principal-1");
        assertThat(cards.getFirst().delegatedToUserId()).isEqualTo("delegate-1");
        assertThat(cards.getFirst().principalCanProcess()).isTrue();
        assertThat(cards.getFirst().currentAssigneeIds()).containsExactly("delegate-1", "principal-1");
        assertThat(cards.getFirst().delegationTaskCount()).isNull();
    }

    @Test
    void shouldBuildTrackingCardsFromInstances() {
        WorkflowInstance instance = instance("instance-1");
        WorkflowTask todo = task("task-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        when(instanceDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of(instance));
        when(taskDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class))).thenReturn(List.of(todo));

        List<WorkflowWorkbenchCard> cards = facade.trackingCards("starter-1", PageRequest.of(1, 20));

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().boardType()).isEqualTo("TRACKING");
        assertThat(cards.getFirst().taskId()).isNull();
        assertThat(cards.getFirst().currentAssigneeIds()).containsExactly("user-1");
    }

    @Test
    void shouldBuildDelegationWorkbenchCardsWithDelegationTaskCount() {
        WorkflowTask first = delegatedTask("task-1", "delegate-1");
        first.setCreatedAt(Instant.parse("2026-06-05T01:00:00Z"));
        WorkflowTask second = delegatedTask("task-2", "delegate-2");
        second.setCreatedAt(Instant.parse("2026-06-05T02:00:00Z"));
        when(taskDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of(first, second));
        when(instanceDao.findById("instance-1")).thenReturn(instance("instance-1"));
        when(nodeDao.findById("node-1")).thenReturn(node("node-1", "approve"));

        List<WorkflowWorkbenchCard> cards = facade.delegationCards("principal-1", PageRequest.of(1, 20));

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().boardType()).isEqualTo("DELEGATION");
        assertThat(cards.getFirst().delegationTaskCount()).isEqualTo(2);
        assertThat(cards.getFirst().currentAssigneeIds()).containsExactly("delegate-1", "delegate-2");
        assertThat(cards.getFirst().delegatedFromUserId()).isEqualTo("principal-1");
    }

    @Test
    void shouldFilterNoticeCardsByReadStatusAndReturnReadStatusOnCard() {
        WorkflowTask unread = task("notice-1", WorkflowTaskKind.NOTICE, WorkflowTaskStatus.TODO);
        WorkflowTask read = task("notice-2", WorkflowTaskKind.NOTICE, WorkflowTaskStatus.NOTICED);
        read.setCreatedAt(Instant.parse("2026-06-05T02:00:00Z"));
        when(taskDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class)))
                .thenReturn(List.of(unread, read));
        when(instanceDao.findById("instance-1")).thenReturn(instance("instance-1"));
        when(nodeDao.findById("node-1")).thenReturn(node("node-1", "notice"));
        WorkflowWorkbenchQueryRequest request = new WorkflowWorkbenchQueryRequest(null, null, null, null, null,
                null, null, null, null, null, null, WorkflowNoticeReadStatus.READ,
                null, null, null, null, null, null, null, null, null, null, List.of());

        List<WorkflowWorkbenchCard> cards = facade.noticeCards("user-1", PageRequest.of(1, 20), request);

        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().taskId()).isEqualTo("notice-2");
        assertThat(cards.getFirst().readStatus()).isEqualTo(WorkflowNoticeReadStatus.READ);
    }

    @Test
    void shouldBuildTodoDoneAndNoticeStatsWithStableBuckets() {
        WorkflowTask normalTodo = task("task-1", WorkflowTaskKind.BUSINESS, WorkflowTaskStatus.TODO);
        normalTodo.setNodeInstanceId("node-normal");
        WorkflowTask warnedTodo = task("task-2", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        warnedTodo.setNodeInstanceId("node-warned");
        WorkflowNodeInstance normalNode = node("node-normal", "visit");
        WorkflowNodeInstance warnedNode = node("node-warned", "approve");
        warnedNode.setOvertimeStatus(WorkflowOvertimeStatus.WARNED);
        when(taskDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of(normalTodo, warnedTodo));
        when(instanceDao.findById("instance-1")).thenReturn(instance("instance-1"));
        when(nodeDao.findById("node-normal")).thenReturn(normalNode);
        when(nodeDao.findById("node-warned")).thenReturn(warnedNode);

        WorkflowWorkbenchStats todoStats = facade.workbenchStats("todo", "user-1");

        assertThat(todoStats.boardType()).isEqualTo("TODO");
        assertThat(todoStats.items()).extracting(WorkflowWorkbenchStatItem::code)
                .containsExactly("ALL", "NORMAL", "WARNED", "OVERDUE");
        assertThat(count(todoStats, "ALL")).isEqualTo(2);
        assertThat(count(todoStats, "WARNED")).isEqualTo(1);

        WorkflowTask done = task("task-3", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.DONE);
        done.setActualProcessorId("user-1");
        WorkflowTask rejected = task("task-4", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.REJECTED);
        rejected.setActualProcessorId("user-1");
        WorkflowTask transferred = task("task-5", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TRANSFERRED);
        transferred.setTransferredBy("user-1");
        when(taskDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of(done, rejected, transferred));

        WorkflowWorkbenchStats doneStats = facade.workbenchStats("done", "user-1");

        assertThat(doneStats.items()).extracting(WorkflowWorkbenchStatItem::code)
                .containsExactly("ALL", "DONE", "REJECTED", "ROLLED_BACK", "TRANSFERRED");
        assertThat(count(doneStats, "ALL")).isEqualTo(3);
        assertThat(count(doneStats, "REJECTED")).isEqualTo(1);
        assertThat(count(doneStats, "TRANSFERRED")).isEqualTo(1);

        WorkflowTask unread = task("notice-1", WorkflowTaskKind.NOTICE, WorkflowTaskStatus.TODO);
        WorkflowTask read = task("notice-2", WorkflowTaskKind.NOTICE, WorkflowTaskStatus.NOTICED);
        when(taskDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class)))
                .thenReturn(List.of(unread, read));

        WorkflowWorkbenchStats noticeStats = facade.workbenchStats("notice", "user-1");

        assertThat(noticeStats.items()).extracting(WorkflowWorkbenchStatItem::code)
                .containsExactly("ALL", "UNREAD", "READ");
        assertThat(count(noticeStats, "UNREAD")).isEqualTo(1);
        assertThat(count(noticeStats, "READ")).isEqualTo(1);
    }

    @Test
    void shouldBuildTrackingAndDelegationStats() {
        WorkflowInstance running = instance("instance-1");
        WorkflowInstance completed = instance("instance-2");
        completed.setInstanceStatus(WorkflowInstanceStatus.COMPLETED);
        when(instanceDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of(running, completed));
        when(taskDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class)))
                .thenReturn(List.of());

        WorkflowWorkbenchStats trackingStats = facade.workbenchStats("tracking", "starter-1");

        assertThat(count(trackingStats, "RUNNING")).isEqualTo(1);
        assertThat(count(trackingStats, "COMPLETED")).isEqualTo(1);

        WorkflowTask delegated = delegatedTask("task-1", "delegate-1");
        WorkflowNodeInstance node = node("node-1", "approve");
        node.setOvertimeStatus(WorkflowOvertimeStatus.OVERDUE);
        when(taskDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of(delegated));
        when(instanceDao.findById("instance-1")).thenReturn(instance("instance-1"));
        when(nodeDao.findById("node-1")).thenReturn(node);

        WorkflowWorkbenchStats delegationStats = facade.workbenchStats("delegation", "principal-1");

        assertThat(delegationStats.items()).extracting(WorkflowWorkbenchStatItem::code)
                .containsExactly("ALL", "NORMAL", "WARNED", "OVERDUE");
        assertThat(count(delegationStats, "ALL")).isEqualTo(1);
        assertThat(count(delegationStats, "OVERDUE")).isEqualTo(1);
    }

    @Test
    void shouldRejectMissingInstance() {
        assertThatThrownBy(() -> facade.renderBundle("missing"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("workflow instance not found");
    }

    @Test
    void shouldDelegateInstanceTaskAndEventQueries() {
        when(instanceDao.findById("instance-1")).thenReturn(instance("instance-1"));

        facade.instanceTasks("instance-1");
        facade.instanceEvents("instance-1");

        verify(taskDao).query(any(Criteria.class), any(PageRequest.class), any(Sort.class));
        verify(eventDao).query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class));
        verify(actionPolicyService, times(2)).requireRecordView(any(WorkflowInstance.class));
    }

    private WorkflowInstance instance(String id) {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(id);
        instance.setDefinitionId("definition-1");
        instance.setWorkflowVersionId("version-1");
        instance.setModuleAlias("sales.contract");
        instance.setRecordId("record-1");
        instance.setStartedBy("starter-1");
        instance.setStartedAt(Instant.parse("2026-06-05T00:00:00Z"));
        instance.setInstanceStatus(WorkflowInstanceStatus.RUNNING);
        instance.setApprovalStatus(WorkflowApprovalStatus.PROCESSING);
        instance.setCurrentNodeKeys("approve");
        return instance;
    }

    private WorkflowTask task(String id, WorkflowTaskKind kind, WorkflowTaskStatus status) {
        WorkflowTask task = new WorkflowTask();
        task.setId(id);
        task.setInstanceId("instance-1");
        task.setNodeInstanceId("node-1");
        task.setTaskKind(kind);
        task.setTaskStatus(status);
        task.setAssigneeId("user-1");
        task.setOriginalAssigneeId("user-1");
        task.setAssignmentKind(WorkflowAssignmentKind.NORMAL);
        return task;
    }

    private WorkflowTask delegatedTask(String id, String delegateId) {
        WorkflowTask task = task(id, WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.TODO);
        task.setAssignmentKind(WorkflowAssignmentKind.DELEGATED);
        task.setOriginalAssigneeId("principal-1");
        task.setAssigneeId(delegateId);
        task.setDelegatedFromUserId("principal-1");
        task.setDelegatedToUserId(delegateId);
        task.setPrincipalCanProcess(false);
        task.setDelegationPolicyId("delegation-1");
        return task;
    }

    private WorkflowNodeInstance node(String id, String key) {
        WorkflowNodeInstance node = new WorkflowNodeInstance();
        node.setId(id);
        node.setInstanceId("instance-1");
        node.setNodeKey(key);
        node.setNodeType(WorkflowNodeType.APPROVAL);
        node.setNodeStatus(WorkflowNodeStatus.ACTIVE);
        node.setOvertimeStatus(WorkflowOvertimeStatus.NORMAL);
        return node;
    }

    private WorkflowRouteInstance route() {
        WorkflowRouteInstance route = new WorkflowRouteInstance();
        route.setId("route-1");
        route.setInstanceId("instance-1");
        return route;
    }

    private long count(WorkflowWorkbenchStats stats, String code) {
        return stats.items().stream()
                .filter(item -> code.equals(item.code()))
                .findFirst()
                .map(WorkflowWorkbenchStatItem::count)
                .orElseThrow();
    }
}
