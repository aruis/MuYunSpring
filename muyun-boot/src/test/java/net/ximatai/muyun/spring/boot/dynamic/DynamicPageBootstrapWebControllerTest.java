package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionAvailability;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.menu.MenuPageMode;
import net.ximatai.muyun.spring.platform.ui.PlatformPageBootstrap;
import net.ximatai.muyun.spring.platform.ui.PlatformPageBootstrapService;
import net.ximatai.muyun.spring.platform.ui.PlatformPageEntryContext;
import net.ximatai.muyun.spring.platform.ui.PlatformResolvedPageConfig;
import net.ximatai.muyun.spring.platform.ui.PlatformUiClientType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DynamicPageBootstrapWebControllerTest {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldExposeMenuBootstrapWithDynamicDescriptor() throws Exception {
        PlatformPageBootstrapService bootstrapService = mock(PlatformPageBootstrapService.class);
        DynamicRecordService recordService = mock(DynamicRecordService.class);
        ActiveTenantVerifier activeTenantVerifier = mock(ActiveTenantVerifier.class);
        DynamicPageBootstrapWebController controller =
                new DynamicPageBootstrapWebController(bootstrapService, recordService, activeTenantVerifier);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        PlatformPageBootstrap bootstrap = new PlatformPageBootstrap(
                new PlatformPageEntryContext("menu-1", "crm.customer", MenuPageMode.LIST,
                        "ui-1", "query-1", "{\"source\":\"menu\"}"),
                PlatformUiClientType.APP,
                PlatformResolvedPageConfig.empty()
        );
        PlatformPageBootstrap webBootstrap = new PlatformPageBootstrap(
                new PlatformPageEntryContext("menu-1", "crm.customer", MenuPageMode.LIST,
                        "ui-web", "query-1", "{\"source\":\"menu\"}"),
                PlatformUiClientType.WEB,
                PlatformResolvedPageConfig.empty()
        );
        when(bootstrapService.bootstrapByMenu("menu-1", PlatformUiClientType.APP)).thenReturn(bootstrap);
        when(bootstrapService.bootstrapByMenu("menu-1", PlatformUiClientType.WEB)).thenReturn(webBootstrap);
        DynamicActionDescriptor visibleAction = action("query");
        DynamicActionDescriptor hiddenAction = action("delete");
        when(recordService.describe("crm.customer")).thenReturn(new DynamicModuleDescriptor(
                "crm.customer", "客户", "customer", List.of(visibleAction, hiddenAction),
                List.of(), List.of(), List.of(), List.of()));
        when(recordService.actionAuthorizationAvailability("crm.customer", "query", Set.of()))
                .thenReturn(DynamicActionAvailability.available("query"));
        when(recordService.actionAuthorizationAvailability("crm.customer", "delete", Set.of()))
                .thenReturn(DynamicActionAvailability.unavailable("delete", "denied"));

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            mvc.perform(get("/platform.menu/menu-1/entry").param("clientType", "APP"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.entry.menuId").value("menu-1"))
                    .andExpect(jsonPath("$.entry.moduleAlias").value("crm.customer"))
                    .andExpect(jsonPath("$.clientType").value("APP"))
                    .andExpect(jsonPath("$.moduleDescriptor.moduleAlias").value("crm.customer"))
                    .andExpect(jsonPath("$.moduleDescriptor.actions.length()").value(1))
                    .andExpect(jsonPath("$.moduleDescriptor.actions[0].code").value("query"))
                    .andExpect(jsonPath("$.mainEntityAlias").value("customer"))
                    .andExpect(jsonPath("$.pageConfig").doesNotExist())
                    .andExpect(jsonPath("$.resolvedConfig.uiFields.length()").value(0));
            mvc.perform(get("/platform.menu/menu-1/entry"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clientType").value("WEB"))
                    .andExpect(jsonPath("$.entry.defaultUiConfigId").value("ui-web"));
        }

        verify(activeTenantVerifier, times(2)).verifyActiveTenant("tenant-a");
    }

    private DynamicActionDescriptor action(String code) {
        return new DynamicActionDescriptor(
                code,
                code,
                true,
                EntityActionLevel.LIST,
                EntityActionCategory.STANDARD,
                EntityActionAccessMode.AUTH_REQUIRED,
                true,
                false,
                null,
                false,
                null,
                EntityActionExecutorType.STANDARD,
                null
        );
    }
}
