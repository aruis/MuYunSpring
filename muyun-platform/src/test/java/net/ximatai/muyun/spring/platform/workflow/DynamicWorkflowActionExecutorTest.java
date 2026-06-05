package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionResultBody;
import net.ximatai.muyun.spring.platform.module.ModuleActionBindingType;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicWorkflowActionExecutorTest {
    private final WorkflowModuleSubmitService submitService = mock(WorkflowModuleSubmitService.class);
    private final WorkflowTaskActionFacade taskActionFacade = mock(WorkflowTaskActionFacade.class);
    private final PlatformModuleActionService actionService = mock(PlatformModuleActionService.class);
    private final DynamicWorkflowActionExecutor executor =
            new DynamicWorkflowActionExecutor(submitService, taskActionFacade, actionService);

    @Test
    void shouldSubmitApprovalByDynamicRecordAction() {
        DynamicActionExecutionRequest request = DynamicActionExecutionRequest.id("record-1")
                .withPayload(Map.of("workflowAction", "submitApproval"));

        Object result = executor.execute(context("submitApproval", "record-1"), request);

        verify(submitService).submitApproval("sales.contract", "record-1", null, null);
        assertThat(result).isInstanceOf(DynamicActionResultBody.class);
        assertThat(((DynamicActionResultBody) result).refresh()).isTrue();
    }

    @Test
    void shouldMapSelectedDirectLinkKeyAliasForApprovalSubmit() {
        DynamicActionExecutionRequest request = DynamicActionExecutionRequest.id("record-1")
                .withPayload(Map.of(
                        "workflowAction", "submitApproval",
                        "selectedDirectLinkKey", "leftRoute",
                        "selectedReason", "choose left"
                ));

        executor.execute(context("submitApproval", "record-1"), request);

        verify(submitService).submitApproval("sales.contract", "record-1", "leftRoute", "choose left");
    }

    @Test
    void shouldSubmitWorkflowByDefinitionAlias() {
        DynamicActionExecutionRequest request = DynamicActionExecutionRequest.id("record-1")
                .withPayload(Map.of("workflowAction", "submitWorkflow", "definitionAlias", "sync"));

        executor.execute(context("submitWorkflow", "record-1"), request);

        verify(submitService).submitWorkflow("sales.contract", "record-1", "sync", null, null);
    }

    @Test
    void shouldRejectPayloadWorkflowActionOverride() {
        DynamicActionExecutionRequest request = DynamicActionExecutionRequest.id("record-1")
                .withPayload(Map.of("workflowAction", "submitWorkflow", "definitionAlias", "sync"));

        assertThatThrownBy(() -> executor.execute(context("customWorkflow", "record-1"), request))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("unsupported dynamic workflow action");
    }

    @Test
    void shouldSubmitWorkflowByBoundModuleAction() {
        PlatformModuleAction action = new PlatformModuleAction();
        action.setBindingType(ModuleActionBindingType.WORKFLOW_DEFINITION);
        action.setBindingAlias("sync");
        when(actionService.findByModuleAliasAndActionCode("sales.contract", "syncWorkflow")).thenReturn(action);
        DynamicActionExecutionRequest request = DynamicActionExecutionRequest.id("record-1");

        executor.execute(context("syncWorkflow", "record-1"), request);

        verify(submitService).submitWorkflow("sales.contract", "record-1", "sync", null, null);
    }

    @Test
    void shouldRouteTaskActionThroughFacade() {
        DynamicActionExecutionRequest request = DynamicActionExecutionRequest.empty()
                .withPayload(Map.of(
                        "workflowAction", "taskAction",
                        "taskActionCode", "reject",
                        "taskId", "task-1",
                        "operatorId", "manager-1",
                        "rejectResubmitMode", "return_to_me",
                        "reason", "not ok",
                        "operatedAt", "2026-06-05T02:00:00Z"
                ));

        executor.execute(context("taskAction", null), request);

        verify(taskActionFacade).execute(org.mockito.ArgumentMatchers.eq("reject"),
                org.mockito.ArgumentMatchers.argThat(actionRequest ->
                        "task-1".equals(actionRequest.taskId())
                                && "manager-1".equals(actionRequest.operatorId())
                                && actionRequest.rejectResubmitMode() == WorkflowRejectResubmitMode.RETURN_TO_ME
                                && "not ok".equals(actionRequest.reason())
                                && Instant.parse("2026-06-05T02:00:00Z").equals(actionRequest.operatedAt())));
    }

    @Test
    void shouldQueryAvailableTaskActions() {
        List<WorkflowTaskAvailableAction> actions = List.of(WorkflowTaskAvailableAction.of("approve", "通过"));
        when(taskActionFacade.availableActions("task-1", "user-1")).thenReturn(actions);
        DynamicActionExecutionRequest request = DynamicActionExecutionRequest.empty()
                .withPayload(Map.of("workflowAction", "availableTaskActions", "taskId", "task-1",
                        "operatorId", "user-1"));

        Object result = executor.execute(context("availableTaskActions", null), request);

        assertThat(((DynamicActionResultBody) result).value()).isSameAs(actions);
    }

    @Test
    void shouldRejectUnsupportedWorkflowAction() {
        DynamicActionExecutionRequest request = DynamicActionExecutionRequest.empty()
                .withPayload(Map.of("workflowAction", "unknown"));

        assertThatThrownBy(() -> executor.execute(context("workflow", null), request))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("unsupported dynamic workflow action");
    }

    private DynamicActionExecutionContext context(String actionCode, String recordId) {
        return new DynamicActionExecutionContext("sales.contract", "contract", actionCode, null,
                recordId, "trace-1", "tenant-1", false, null);
    }
}
