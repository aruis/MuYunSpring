package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelColumnPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelSheetPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookPlan;
import net.ximatai.muyun.spring.platform.exchange.template.DynamicExchangeTemplatePlanBuilder;
import net.ximatai.muyun.spring.platform.exchange.writer.ExcelWorkbookPlanWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DynamicExchangeTemplateWebControllerTest {
    private static final String MODULE = "sales.order";

    private DynamicRecordService recordService;
    private ActiveTenantVerifier activeTenantVerifier;
    private DynamicExchangeTemplatePlanBuilder templatePlanBuilder;
    private ExcelWorkbookPlanWriter writer;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        recordService = mock(DynamicRecordService.class);
        activeTenantVerifier = mock(ActiveTenantVerifier.class);
        templatePlanBuilder = mock(DynamicExchangeTemplatePlanBuilder.class);
        writer = mock(ExcelWorkbookPlanWriter.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new DynamicExchangeTemplateWebController(
                        recordService, activeTenantVerifier, templatePlanBuilder, writer))
                .addFilters(new CurrentUserWebFilter(() -> java.util.Optional.of(
                        CurrentUser.tenantUser("user-1", "User", "tenant_a"))))
                .build();
    }

    @Test
    void shouldDownloadExchangeTemplateWorkbook() throws Exception {
        DynamicModuleDescriptor descriptor = descriptor();
        ExcelWorkbookPlan plan = plan();
        when(recordService.describe(MODULE)).thenReturn(descriptor);
        when(templatePlanBuilder.build(descriptor)).thenReturn(plan);
        when(writer.writeToBytes(plan)).thenReturn(new byte[]{1, 2, 3});

        mvc.perform(post("/{moduleAlias}/exchange/template", MODULE))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Exchange-FileName", "sales_order-exchange-template.xlsx"))
                .andExpect(header().string("Access-Control-Expose-Headers",
                        "Content-Disposition,X-Exchange-FileName"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));

        verify(activeTenantVerifier).verifyActiveTenant("tenant_a");
    }

    @Test
    void shouldRejectTemplateWhenModuleDoesNotSupportExchange() throws Exception {
        when(recordService.describe(MODULE)).thenReturn(descriptorWithoutExchange());

        mvc.perform(post("/{moduleAlias}/exchange/template", MODULE))
                .andExpect(status().isBadRequest());
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
