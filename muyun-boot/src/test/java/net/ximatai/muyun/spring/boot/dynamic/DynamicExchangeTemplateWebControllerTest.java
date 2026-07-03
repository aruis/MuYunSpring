package net.ximatai.muyun.spring.boot.dynamic;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
                "template", String.class, DynamicExchangeTemplateRequest.class, HttpServletResponse.class);
        assertThat(method.getAnnotation(POST.class)).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo("/template");
        assertThat(method.getParameters()[0].getAnnotation(PathParam.class).value()).isEqualTo("moduleAlias");
        assertThat(method.getParameters()[2].getAnnotation(Context.class)).isNotNull();

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
        CapturingResponse response = new CapturingResponse();

        inTenant(() -> controller.template(MODULE, new DynamicExchangeTemplateRequest(
                List.of("order.customerId"), 100), response.mock()));

        assertThat(response.contentType).isEqualTo(DynamicImportWebController.XLSX_CONTENT_TYPE);
        assertThat(response.headers).containsEntry("X-Exchange-FileName", "sales_order-exchange-template.xlsx");
        assertThat(response.headers).containsEntry("Access-Control-Expose-Headers",
                "Content-Disposition,X-Exchange-FileName");
        assertThat(response.contentLength).isEqualTo(3);
        assertThat(response.bytes()).containsExactly(1, 2, 3);
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
        CapturingResponse response = new CapturingResponse();

        assertThatThrownBy(() -> inTenant(() -> controller.template(MODULE, null, response.mock())))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("dynamic entity does not support capability: EXCHANGE");
    }

    private void inTenant(Runnable action) {
        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            action.run();
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

    private static final class CapturingResponse {
        private final HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();
        private final java.util.Map<String, String> headers = new java.util.LinkedHashMap<>();
        private String contentType;
        private int contentLength;

        private CapturingResponse() throws IOException {
            when(response.getOutputStream()).thenReturn(new ServletOutputStream() {
                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {
                }

                @Override
                public void write(int b) {
                    body.write(b);
                }
            });
            org.mockito.Mockito.doAnswer(invocation -> {
                contentType = invocation.getArgument(0);
                return null;
            }).when(response).setContentType(any());
            org.mockito.Mockito.doAnswer(invocation -> {
                headers.put(invocation.getArgument(0), invocation.getArgument(1));
                return null;
            }).when(response).setHeader(any(), any());
            org.mockito.Mockito.doAnswer(invocation -> {
                contentLength = invocation.getArgument(0);
                return null;
            }).when(response).setContentLength(org.mockito.ArgumentMatchers.anyInt());
        }

        private HttpServletResponse mock() {
            return response;
        }

        private byte[] bytes() {
            return body.toByteArray();
        }
    }
}
