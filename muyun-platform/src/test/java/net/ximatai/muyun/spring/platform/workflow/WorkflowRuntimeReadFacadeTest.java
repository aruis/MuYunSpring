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
    private final WorkflowRuntimeReadFacade facade = new WorkflowRuntimeReadFacade(
            instanceDao, taskDao, nodeDao, routeDao, eventDao, availabilityService);

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
    }

    private WorkflowInstance instance(String id) {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(id);
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
}
