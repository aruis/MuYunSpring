package net.ximatai.muyun.spring.boot.dynamic;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionAvailability;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.platform.menu.MenuPageMode;
import net.ximatai.muyun.spring.platform.ui.PlatformActionBlock;
import net.ximatai.muyun.spring.platform.ui.PlatformPageBootstrap;
import net.ximatai.muyun.spring.platform.ui.PlatformPageBootstrapService;
import net.ximatai.muyun.spring.platform.ui.PlatformPageEntryContext;
import net.ximatai.muyun.spring.platform.ui.PlatformResolvedPageConfig;
import net.ximatai.muyun.spring.platform.ui.PlatformUiClientType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicPageBootstrapWebControllerTest {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldDeclareMenuBootstrapRoute() throws Exception {
        assertThat(DynamicPageBootstrapWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/platform.menu");

        Method entry = DynamicPageBootstrapWebController.class.getMethod(
                "entry", String.class, PlatformUiClientType.class);
        assertThat(entry.getAnnotation(GET.class)).isNotNull();
        assertThat(entry.getAnnotation(Path.class).value()).isEqualTo("/{menuId}/entry");
        assertThat(entry.getParameters()[0].getAnnotation(PathParam.class).value()).isEqualTo("menuId");
        assertThat(entry.getParameters()[1].getAnnotation(QueryParam.class).value()).isEqualTo("clientType");
        assertThat(entry.getParameters()[1].getAnnotation(DefaultValue.class).value()).isEqualTo("WEB");
    }

    @Test
    void shouldExposeMenuBootstrapWithDynamicDescriptor() {
        PlatformPageBootstrapService bootstrapService = mock(PlatformPageBootstrapService.class);
        DynamicRecordService recordService = mock(DynamicRecordService.class);
        TenantService activeTenantVerifier = mock(TenantService.class);
        DynamicPageBootstrapWebController controller =
                new DynamicPageBootstrapWebController(bootstrapService, recordService, activeTenantVerifier);
        PlatformPageBootstrap bootstrap = new PlatformPageBootstrap(
                new PlatformPageEntryContext("menu-1", "crm.customer", MenuPageMode.LIST,
                        "ui-1", "query-1", "{\"source\":\"menu\"}"),
                PlatformUiClientType.APP,
                new PlatformResolvedPageConfig(List.of(), List.of(), List.of(), List.of(), List.of(
                        new PlatformActionBlock("ui-1", "action", null, "query", null, "toolbar"),
                        new PlatformActionBlock("ui-1", "action", null, "delete", null, "toolbar")
                ), List.of())
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
            DynamicPageBootstrapResponse appResponse = controller.entry("menu-1", PlatformUiClientType.APP);
            DynamicPageBootstrapResponse webResponse = controller.entry("menu-1", PlatformUiClientType.WEB);

            assertThat(appResponse.entry().menuId()).isEqualTo("menu-1");
            assertThat(appResponse.entry().moduleAlias()).isEqualTo("crm.customer");
            assertThat(appResponse.clientType()).isEqualTo(PlatformUiClientType.APP);
            assertThat(appResponse.moduleDescriptor().moduleAlias()).isEqualTo("crm.customer");
            assertThat(appResponse.moduleDescriptor().actions()).singleElement()
                    .extracting(DynamicActionDescriptor::code)
                    .isEqualTo("query");
            assertThat(appResponse.mainEntityAlias()).isEqualTo("customer");
            assertThat(appResponse.openApiPath()).isEqualTo("/crm.customer/openapi");
            assertThat(appResponse.resolvedConfig().uiFields()).isEmpty();
            assertThat(appResponse.resolvedConfig().actionBlocks()).singleElement()
                    .extracting(PlatformActionBlock::actionCode)
                    .isEqualTo("query");
            assertThat(webResponse.clientType()).isEqualTo(PlatformUiClientType.WEB);
            assertThat(webResponse.entry().defaultUiConfigId()).isEqualTo("ui-web");
        }

        verify(activeTenantVerifier, times(2)).verifyActiveTenant("tenant-a");
    }

    @Test
    void shouldRequireTenantContext() {
        DynamicPageBootstrapWebController controller = new DynamicPageBootstrapWebController(
                mock(PlatformPageBootstrapService.class),
                mock(DynamicRecordService.class),
                mock(TenantService.class));

        assertThatThrownBy(() -> controller.entry("menu-1", PlatformUiClientType.WEB))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("page bootstrap requires tenant context");
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
