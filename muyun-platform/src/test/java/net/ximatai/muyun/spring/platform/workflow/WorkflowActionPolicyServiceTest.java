package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WorkflowActionPolicyServiceTest {
    private final ActionExecutionPolicyService executionPolicyService = mock(ActionExecutionPolicyService.class);
    private final WorkflowActionPolicyService service = new WorkflowActionPolicyService(executionPolicyService);

    @Test
    void shouldRequireRuntimeActionThroughUnifiedActionPolicy() {
        WorkflowInstance instance = instance();

        service.requireRuntimeAction(instance, "approve");

        ArgumentCaptor<ActionExecutionContext> context = ArgumentCaptor.forClass(ActionExecutionContext.class);
        verify(executionPolicyService).requireRecordAction(context.capture());
        assertThat(context.getValue().moduleAlias()).isEqualTo("sales.contract");
        assertThat(context.getValue().actionCode()).isEqualTo("approve");
        assertThat(context.getValue().recordIds()).containsExactly("record-1");
        assertThat(context.getValue().actionPolicy().level()).isEqualTo(PlatformActionLevel.RECORD);
        assertThat(context.getValue().actionPolicy().accessMode()).isEqualTo(ActionAccessMode.LOGIN_REQUIRED);
        assertThat(context.getValue().actionPolicy().actionAuth()).isFalse();
        assertThat(context.getValue().actionPolicy().dataAuth()).isFalse();
    }

    @Test
    void shouldPropagateRuntimeActionAuthorizationFailure() {
        doThrow(new PlatformException("workflow action denied")).when(executionPolicyService)
                .requireRecordAction(org.mockito.ArgumentMatchers.any());

        assertThatThrownBy(() -> service.requireRuntimeAction(instance(), "approve"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("workflow action denied");
    }

    private WorkflowInstance instance() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId("instance-1");
        instance.setModuleAlias("sales.contract");
        instance.setRecordId("record-1");
        return instance;
    }
}
