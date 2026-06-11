package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.ui.PlatformModuleTaskCheckService;
import net.ximatai.muyun.spring.platform.ui.PlatformModuleTaskStatus;
import net.ximatai.muyun.spring.platform.ui.PlatformTaskCheckType;
import net.ximatai.muyun.spring.platform.ui.PlatformTaskCompletionStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DynamicModuleTaskWebControllerTest {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCheckModuleTasksByCurrentRecordAndUiConfig() throws Exception {
        PlatformModuleTaskCheckService taskCheckService = mock(PlatformModuleTaskCheckService.class);
        DynamicRecordService recordService = mock(DynamicRecordService.class);
        ActiveTenantVerifier activeTenantVerifier = mock(ActiveTenantVerifier.class);
        when(recordService.mainEntityAlias("crm.customer")).thenReturn("customer");
        when(taskCheckService.check("crm.customer", "customer-1", "ui-detail"))
                .thenReturn(List.of(new PlatformModuleTaskStatus(
                        "contracts",
                        "合同齐备",
                        PlatformTaskCheckType.ASSOCIATION_VIEW,
                        PlatformTaskCompletionStatus.COMPLETE,
                        2L,
                        "/crm.customer/view/{id}/associations/contracts/query",
                        null
                )));
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new DynamicModuleTaskWebController(taskCheckService, recordService, activeTenantVerifier))
                .build();

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            mvc.perform(post("/crm.customer/view/customer-1/tasks/check")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"uiConfigId\":\"ui-detail\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].key").value("contracts"))
                    .andExpect(jsonPath("$[0].status").value("COMPLETE"))
                    .andExpect(jsonPath("$[0].matchedCount").value(2));
        }

        verify(activeTenantVerifier).verifyActiveTenant("tenant-a");
        verify(recordService).requireRecordActionScope(eq("crm.customer"), eq("customer"),
                any(ActionExecutionPolicy.class), eq(List.of("customer-1")), eq(Optional.empty()));
        verify(taskCheckService).check("crm.customer", "customer-1", "ui-detail");
    }
}
