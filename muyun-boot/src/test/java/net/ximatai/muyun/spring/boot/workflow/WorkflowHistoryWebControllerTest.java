package net.ximatai.muyun.spring.boot.workflow;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowAssignmentKind;
import net.ximatai.muyun.spring.platform.workflow.WorkflowEvent;
import net.ximatai.muyun.spring.platform.workflow.WorkflowEventType;
import net.ximatai.muyun.spring.platform.workflow.WorkflowHistoryEventView;
import net.ximatai.muyun.spring.platform.workflow.WorkflowHistoryInstance;
import net.ximatai.muyun.spring.platform.workflow.WorkflowHistoryQueryService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowHistoryTaskView;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRuntimeRenderBundle;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTask;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskKind;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowHistoryWebControllerTest {
    private final WorkflowHistoryQueryService historyQueryService = mock(WorkflowHistoryQueryService.class);
    private final WorkflowHistoryWebController controller = new WorkflowHistoryWebController(historyQueryService);

    @Test
    void shouldDeclareHistoryRoutes() throws Exception {
        assertThat(WorkflowHistoryWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/workflow/history");

        assertRoute("query", new Class<?>[]{WorkflowHistoryWebController.WorkflowHistoryQueryWebRequest.class},
                POST.class, "/query");
        assertRoute("renderBundle", new Class<?>[]{String.class}, GET.class, "/{historyInstanceId}/bundle");
        assertRoute("tasks", new Class<?>[]{String.class}, GET.class, "/{historyInstanceId}/tasks");
        assertRoute("taskViews", new Class<?>[]{String.class}, GET.class, "/{historyInstanceId}/tasks/view");
        assertRoute("events", new Class<?>[]{String.class}, GET.class, "/{historyInstanceId}/events");
        assertRoute("eventViews", new Class<?>[]{String.class}, GET.class, "/{historyInstanceId}/events/view");
    }

    @Test
    void shouldQueryWorkflowHistoryByRecord() {
        WorkflowHistoryInstance history = new WorkflowHistoryInstance();
        history.setId("history-1");
        history.setModuleAlias("sales.contract");
        history.setRecordId("record-1");
        when(historyQueryService.queryRecordHistory(eq("sales.contract"), eq("record-1"), eq("starter-1"), any()))
                .thenReturn(List.of(history));
        ArgumentCaptor<PageRequest> pageCaptor = ArgumentCaptor.forClass(PageRequest.class);

        WebListResponse<WorkflowHistoryInstance> response = controller.query(
                new WorkflowHistoryWebController.WorkflowHistoryQueryWebRequest(
                        "sales.contract",
                        "record-1",
                        "starter-1",
                        new WebPageRequest(2, 10)
                )
        );

        assertThat(response.records()).containsExactly(history);
        verify(historyQueryService).queryRecordHistory(eq("sales.contract"), eq("record-1"), eq("starter-1"),
                pageCaptor.capture());
        assertThat(pageCaptor.getValue().getOffset()).isEqualTo(10);
        assertThat(pageCaptor.getValue().getLimit()).isEqualTo(10);
    }

    @Test
    void shouldUseDefaultPageWhenHistoryQueryPayloadOrPageIsMissing() {
        when(historyQueryService.queryRecordHistory(eq(null), eq(null), eq(null), any()))
                .thenReturn(List.of());
        ArgumentCaptor<PageRequest> pageCaptor = ArgumentCaptor.forClass(PageRequest.class);

        WebListResponse<WorkflowHistoryInstance> response = controller.query(null);

        assertThat(response.records()).isEmpty();
        verify(historyQueryService).queryRecordHistory(eq(null), eq(null), eq(null), pageCaptor.capture());
        assertThat(pageCaptor.getValue().getOffset()).isEqualTo(0);
        assertThat(pageCaptor.getValue().getLimit()).isEqualTo(WebPageRequest.DEFAULT.pageSize());
    }

    @Test
    void shouldExposeHistoryBundleTasksAndEvents() {
        WorkflowRuntimeRenderBundle bundle = new WorkflowRuntimeRenderBundle("HISTORY", null, List.of(), List.of());
        WorkflowHistoryTaskView taskView = new WorkflowHistoryTaskView(
                "task-1", "instance-1", "node-1", WorkflowTaskKind.APPROVAL, WorkflowTaskStatus.DONE,
                WorkflowAssignmentKind.DELEGATED, "delegate-1", "代理人", "delegate-1", "代理人", true,
                "principal-1", "原审批人", "principal-1", "原审批人", "delegate-1", "代理人", true,
                "delegation-1", "{}", false, false, "approve", null, null);
        WorkflowHistoryEventView eventView = new WorkflowHistoryEventView(
                WorkflowHistoryEventView.ORIGIN_TYPE_ADD_SIGN, true, "approve-source", "approve-source",
                "event-1", "instance-1", "node-1", "task-1", WorkflowEventType.TASK_COMPLETED, "approve",
                "delegate-1", "代理人", "delegate-1", "代理人", true, WorkflowAssignmentKind.DELEGATED,
                "principal-1", "原审批人", "principal-1", "原审批人", "delegate-1", "代理人", true,
                "delegation-1", "{}", false, false, null, null, null);
        WorkflowTask task = new WorkflowTask();
        WorkflowEvent event = new WorkflowEvent();
        when(historyQueryService.renderBundle("history-1")).thenReturn(bundle);
        when(historyQueryService.tasks("history-1")).thenReturn(List.of(task));
        when(historyQueryService.events("history-1")).thenReturn(List.of(event));
        when(historyQueryService.taskViews("history-1")).thenReturn(List.of(taskView));
        when(historyQueryService.eventViews("history-1")).thenReturn(List.of(eventView));

        assertThat(controller.renderBundle("history-1")).isSameAs(bundle);
        assertThat(controller.tasks("history-1").records()).containsExactly(task);
        assertThat(controller.events("history-1").records()).containsExactly(event);
        assertThat(controller.taskViews("history-1").records()).singleElement().satisfies(value -> {
            assertThat(value.actualProcessUserId()).isEqualTo("delegate-1");
            assertThat(value.actualProcessUserTitle()).isEqualTo("代理人");
            assertThat(value.processedByDelegation()).isTrue();
        });
        assertThat(controller.eventViews("history-1").records()).singleElement().satisfies(value -> {
            assertThat(value.originType()).isEqualTo(WorkflowHistoryEventView.ORIGIN_TYPE_ADD_SIGN);
            assertThat(value.isAddSignRoute()).isTrue();
            assertThat(value.addSignSourceNodeKey()).isEqualTo("approve-source");
            assertThat(value.operatorTitle()).isEqualTo("代理人");
            assertThat(value.delegationPolicyId()).isEqualTo("delegation-1");
        });
    }

    @Test
    void shouldNotDeclareHistoryDeleteRoute() {
        assertThat(Arrays.stream(WorkflowHistoryWebController.class.getMethods())
                .filter(method -> method.getAnnotation(Path.class) != null)
                .map(method -> method.getAnnotation(Path.class).value()))
                .doesNotContain("/{historyInstanceId}/delete", "/delete/{historyInstanceId}");
    }

    private void assertRoute(String methodName,
                             Class<?>[] parameterTypes,
                             Class<?> httpMethod,
                             String path) throws Exception {
        Method method = WorkflowHistoryWebController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(httpMethod.asSubclass(java.lang.annotation.Annotation.class))).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo(path);
    }
}
