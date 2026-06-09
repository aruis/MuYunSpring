package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.common.platform.PlatformAction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DynamicRecordActionGatewayTest {
    private static final String MODULE = "sales.order";

    @Test
    void shouldRequireListLevelActionWhenCreatingGateway() {
        DynamicRecordService recordService = mock(DynamicRecordService.class);

        assertThatThrownBy(() -> new DynamicRecordActionGateway(
                recordService, MODULE, PlatformAction.UPDATE, "trace-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("list-level action");
    }

    @Test
    void shouldRejectUnsupportedListLevelActionWhenCreatingGateway() {
        DynamicRecordService recordService = mock(DynamicRecordService.class);

        assertThatThrownBy(() -> new DynamicRecordActionGateway(
                recordService, MODULE, PlatformAction.CREATE, "trace-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not support action");
    }

    @Test
    void shouldAuthorizeActionWhenCreatingGateway() {
        DynamicRecordService recordService = mock(DynamicRecordService.class);

        new DynamicRecordActionGateway(recordService, MODULE, PlatformAction.IMPORT, "dynamic-import");

        verify(recordService).requireAction(MODULE, PlatformAction.IMPORT);
    }

    @Test
    void shouldRejectMutationForReadOnlyExchangeAction() {
        DynamicRecordService recordService = mock(DynamicRecordService.class);
        DynamicRecordActionGateway gateway = new DynamicRecordActionGateway(
                recordService, MODULE, PlatformAction.EXPORT, "dynamic-export");

        assertThatThrownBy(() -> gateway.create("order", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not allow mutation");
    }

    @Test
    void shouldRequireActiveTransactionForMutationAction() {
        DynamicRecordService recordService = mock(DynamicRecordService.class);
        DynamicRecordActionGateway gateway = new DynamicRecordActionGateway(
                recordService, MODULE, PlatformAction.IMPORT, "dynamic-import");

        assertThatThrownBy(() -> gateway.create("order", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires active transaction");
    }
}
