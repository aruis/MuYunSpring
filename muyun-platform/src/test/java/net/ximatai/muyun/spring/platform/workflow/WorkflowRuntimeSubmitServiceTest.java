package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class WorkflowRuntimeSubmitServiceTest {
    private final WorkflowSubmitDraftService draftService = mock(WorkflowSubmitDraftService.class);
    private final WorkflowInstanceService instanceService = mock(WorkflowInstanceService.class);
    private final WorkflowInstanceDao instanceDao = mock(WorkflowInstanceDao.class);
    private final WorkflowNodeInstanceDao nodeDao = mock(WorkflowNodeInstanceDao.class);
    private final WorkflowRouteInstanceDao routeDao = mock(WorkflowRouteInstanceDao.class);
    private final WorkflowTaskDao taskDao = mock(WorkflowTaskDao.class);
    private final WorkflowEventDao eventDao = mock(WorkflowEventDao.class);
    private final WorkflowRuntimeSubmitService service = new WorkflowRuntimeSubmitService(
            draftService, instanceService, instanceDao, nodeDao, routeDao, taskDao, eventDao);

    @Test
    void shouldPersistSubmitDraftInStableOrder() {
        WorkflowSubmitDraft draft = draft();

        service.persist(draft, Instant.parse("2026-06-05T01:00:00Z"));

        verify(instanceDao).insert(draft.instance());
        verify(instanceService).beforeInsert(draft.instance());
        verify(nodeDao).insert(draft.nodes().get(0));
        verify(routeDao).insert(draft.routes().get(0));
        verify(taskDao).insert(draft.tasks().get(0));
        verify(eventDao).insert(draft.events().get(0));
        assertThat(draft.instance().getCreatedAt()).isEqualTo(Instant.parse("2026-06-05T01:00:00Z"));
        assertThat(draft.tasks().get(0).getVersion()).isZero();
    }

    @Test
    void shouldBuildAndPersistSubmitDraft() {
        WorkflowDefinition definition = new WorkflowDefinition();
        WorkflowVersion version = new WorkflowVersion();
        WorkflowSubmitDraft draft = draft();
        when(draftService.build(definition, version, List.of(), List.of(), "record-1", "user-1",
                Instant.parse("2026-06-05T01:00:00Z"))).thenReturn(draft);

        WorkflowSubmitDraft result = service.submit(definition, version, List.of(), List.of(),
                "record-1", "user-1", Instant.parse("2026-06-05T01:00:00Z"));

        assertThat(result).isEqualTo(draft);
        verify(draftService).build(definition, version, List.of(), List.of(), "record-1", "user-1",
                Instant.parse("2026-06-05T01:00:00Z"));
        verify(instanceDao).insert(draft.instance());
        verify(instanceService).beforeInsert(draft.instance());
        verifyNoMoreInteractions(draftService);
    }

    @Test
    void shouldStopPersistingWhenInstanceValidationFails() {
        WorkflowSubmitDraft draft = draft();
        doThrow(new PlatformException("approval workflow already running for record: record-1"))
                .when(instanceService).beforeInsert(draft.instance());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.persist(draft,
                        Instant.parse("2026-06-05T01:00:00Z")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("approval workflow already running");

        verify(instanceService).beforeInsert(draft.instance());
        verifyNoInteractions(instanceDao, nodeDao, routeDao, taskDao, eventDao);
    }

    private WorkflowSubmitDraft draft() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId("instance-1");
        WorkflowNodeInstance node = new WorkflowNodeInstance();
        node.setId("node-1");
        WorkflowRouteInstance route = new WorkflowRouteInstance();
        route.setId("route-1");
        WorkflowTask task = new WorkflowTask();
        task.setId("task-1");
        WorkflowEvent event = new WorkflowEvent();
        event.setId("event-1");
        return new WorkflowSubmitDraft(instance, List.of(node), List.of(route), List.of(task), List.of(event),
                new WorkflowActivationResult(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), false));
    }
}
