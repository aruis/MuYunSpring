package net.ximatai.muyun.spring.boot.dynamic;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelColumnPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelSheetPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookPlan;
import net.ximatai.muyun.spring.platform.exchange.template.DynamicExchangeTemplateOptions;
import net.ximatai.muyun.spring.platform.exchange.template.DynamicExchangeTemplatePlanBuilder;
import net.ximatai.muyun.spring.platform.exchange.writer.ExcelWorkbookPlanWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicExchangeTemplateWebControllerTest {
    private static final String MODULE = "sales.order";

    private final DynamicRecordService recordService = mock(DynamicRecordService.class);
    private final TenantService activeTenantVerifier = mock(TenantService.class);
    private final DynamicExchangeTemplatePlanBuilder templatePlanBuilder =
            mock(DynamicExchangeTemplatePlanBuilder.class);
    private final ExcelWorkbookPlanWriter writer = mock(ExcelWorkbookPlanWriter.class);
    private final DynamicExchangeTemplateWebController controller = new DynamicExchangeTemplateWebController(
            recordService, activeTenantVerifier, templatePlanBuilder, writer);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldDeclareExchangeTemplateRouteAndActionMetadata() throws Exception {
        assertThat(DynamicExchangeTemplateWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/{moduleAlias:[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+}/exchange");

        Method method = DynamicExchangeTemplateWebController.class.getMethod(
                "template", String.class, DynamicExchangeTemplateRequest.class);
        assertThat(method.getAnnotation(POST.class)).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo("/template");
        assertThat(method.getParameters()[0].getAnnotation(PathParam.class).value()).isEqualTo("moduleAlias");

        ActionEndpoint endpoint = method.getAnnotation(ActionEndpoint.class);
        assertThat(endpoint).isNotNull();
        assertThat(endpoint.value()).isEqualTo(PlatformAction.IMPORT);
    }

    @Test
    void shouldDownloadExchangeTemplateWorkbook() throws Exception {
        DynamicModuleDescriptor descriptor = descriptor();
        ExcelWorkbookPlan plan = plan();
        when(recordService.describe(MODULE)).thenReturn(descriptor);
        when(templatePlanBuilder.build(eq(descriptor), any(DynamicExchangeTemplateOptions.class))).thenReturn(plan);
        when(writer.writeToBytes(plan)).thenReturn(new byte[]{1, 2, 3});

        Response response = inTenant(() -> controller.template(MODULE, new DynamicExchangeTemplateRequest(
                List.of("order.customerId"), 100)));

        assertThat(response.getMediaType().toString()).isEqualTo(DynamicImportWebController.XLSX_CONTENT_TYPE);
        assertThat(response.getHeaderString("X-Exchange-FileName")).isEqualTo("sales_order-exchange-template.xlsx");
        assertThat(response.getHeaderString("Access-Control-Expose-Headers")).isEqualTo(
                "Content-Disposition,X-Exchange-FileName");
        assertThat(response.getHeaderString("Content-Length")).isEqualTo("3");
        assertThat((byte[]) response.getEntity()).containsExactly(1, 2, 3);
        verify(activeTenantVerifier).verifyActiveTenant("tenant_a");
        ArgumentCaptor<DynamicExchangeTemplateOptions> options =
                ArgumentCaptor.forClass(DynamicExchangeTemplateOptions.class);
        verify(templatePlanBuilder).build(eq(descriptor), options.capture());
        assertThat(options.getValue().disabledReferenceDropdownFields()).containsExactly("order.customerId");
        assertThat(options.getValue().referenceDropdownLimit()).isEqualTo(100);
    }

    @Test
    void shouldRejectTemplateWhenModuleDoesNotSupportExchange() throws Exception {
        when(recordService.describe(MODULE)).thenReturn(descriptorWithoutExchange());

        assertThatThrownBy(() -> inTenant(() -> controller.template(MODULE, null)))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("dynamic entity does not support capability: EXCHANGE");
    }

    private <T> T inTenant(java.util.function.Supplier<T> action) {
        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            return action.get();
        }
    }

    private DynamicModuleDescriptor descriptor() {
        return DynamicModuleDescriptor.from(new ModuleDefinition(
                MODULE,
                "Order",
                List.of(new EntityDefinition("order", "sales_order", "Order", List.of(
                        FieldDefinition.string("orderNo", "Order No")
                ), java.util.Set.of(EntityCapability.EXCHANGE)))
        ));
    }

    private DynamicModuleDescriptor descriptorWithoutExchange() {
        return DynamicModuleDescriptor.from(new ModuleDefinition(
                MODULE,
                "Order",
                List.of(new EntityDefinition("order", "sales_order", "Order", List.of(
                        FieldDefinition.string("orderNo", "Order No")
                )))
        ));
    }

    private ExcelWorkbookPlan plan() {
        return new ExcelWorkbookPlan(List.of(new ExcelSheetPlan(
                "Order",
                "order",
                true,
                List.of(new ExcelColumnPlan("orderNo", "Order No"))
        )));
    }

}
