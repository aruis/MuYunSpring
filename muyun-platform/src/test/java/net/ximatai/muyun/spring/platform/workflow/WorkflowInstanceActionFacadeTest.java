package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WorkflowInstanceActionFacadeTest {
    private final WorkflowInstanceActionService actionService = mock(WorkflowInstanceActionService.class);
    private final WorkflowInstanceActionFacade facade = new WorkflowInstanceActionFacade(actionService);

    @Test
    void shouldDispatchInstanceActionsByCode() {
        WorkflowInstanceActionRequest request = WorkflowInstanceActionRequest.revoke("inst-1", "user-1", "cancel");

        facade.execute("revoke", request);
        facade.execute("terminate", request);

        verify(actionService).revoke(request);
        verify(actionService).terminate(request);
    }

    @Test
    void shouldRejectUnsupportedActionCode() {
        WorkflowInstanceActionRequest request = WorkflowInstanceActionRequest.revoke("inst-1", "user-1", "cancel");

        assertThatThrownBy(() -> facade.execute("unknown", request))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("unsupported workflow instance action");
        assertThatThrownBy(() -> facade.execute(" ", request))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("action code");
    }
}
