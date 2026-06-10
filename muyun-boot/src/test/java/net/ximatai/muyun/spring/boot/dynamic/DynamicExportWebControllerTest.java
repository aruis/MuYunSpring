package net.ximatai.muyun.spring.boot.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebQueryCondition;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebSort;
import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicEntityOperations;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.exchange.exporter.DynamicExportCommand;
import net.ximatai.muyun.spring.platform.exchange.exporter.DynamicExportFacade;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigSnapshot;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigSnapshotService;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryItemService;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DynamicExportWebControllerTest {
    private static final String MODULE = "sales.order";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private DynamicRecordService recordService;
    private ActiveTenantVerifier activeTenantVerifier;
    private DynamicExportFacade exportFacade;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        recordService = mock(DynamicRecordService.class);
        activeTenantVerifier = mock(ActiveTenantVerifier.class);
        exportFacade = mock(DynamicExportFacade.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new DynamicExportWebController(
                        recordService, activeTenantVerifier, exportFacade))
                .addFilters(new CurrentUserWebFilter(() -> java.util.Optional.of(
                        CurrentUser.tenantUser("user-1", "User", "tenant_a"))))
                .build();
    }

    @Test
    void shouldExportDataWorkbookThroughFacade() throws Exception {
        DynamicModuleDescriptor descriptor = descriptor();
        DynamicEntityOperations operations = mock(DynamicEntityOperations.class);
        Criteria criteria = Criteria.of().eq("status", "active");
        when(recordService.describe(MODULE)).thenReturn(descriptor);
        when(recordService.mainEntity(MODULE)).thenReturn(operations);
        when(operations.queryCriteria(org.mockito.ArgumentMatchers.anyList())).thenReturn(criteria);
        when(exportFacade.exportWorkbook(org.mockito.ArgumentMatchers.any(DynamicExportCommand.class)))
                .thenReturn(new byte[]{7, 8, 9});

        WebQueryRequest request = new WebQueryRequest(
                new net.ximatai.muyun.spring.boot.web.WebPageRequest(2, 50),
                List.of(new WebQueryCondition("status", "EQ", List.of("active"))),
                List.of(new WebSort("orderNo", true))
        );
        mvc.perform(post("/{moduleAlias}/export/data", MODULE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Export-FileName", "sales_order-export.xlsx"))
                .andExpect(content().bytes(new byte[]{7, 8, 9}));

        ArgumentCaptor<DynamicExportCommand> captor = ArgumentCaptor.forClass(DynamicExportCommand.class);
        verify(exportFacade).exportWorkbook(captor.capture());
        assertThat(captor.getValue().descriptor()).isSameAs(descriptor);
        assertThat(captor.getValue().criteria()).isSameAs(criteria);
        assertThat(captor.getValue().pageRequest().getOffset()).isEqualTo(50);
        assertThat(captor.getValue().pageRequest().getLimit()).isEqualTo(50);
        assertThat(captor.getValue().sorts()).hasSize(1);
    }

    @Test
    void shouldReuseLowCodeQueryTemplateWhenExportingData() throws Exception {
        PlatformPageConfigSnapshotService snapshotService = mock(PlatformPageConfigSnapshotService.class);
        PlatformQueryItemService queryItemService = mock(PlatformQueryItemService.class);
        MockMvc exportMvc = MockMvcBuilders
                .standaloneSetup(new DynamicExportWebController(
                        recordService, activeTenantVerifier, exportFacade, snapshotService, queryItemService))
                .addFilters(new CurrentUserWebFilter(() -> java.util.Optional.of(
                        CurrentUser.tenantUser("user-1", "User", "tenant_a"))))
                .build();
        DynamicModuleDescriptor descriptor = descriptor();
        DynamicEntityOperations operations = mock(DynamicEntityOperations.class);
        PlatformQueryTemplate template = new PlatformQueryTemplate();
        template.setId("tpl-active");
        template.setModuleAlias(MODULE);
        template.setAlias("active");
        Criteria templateCriteria = Criteria.of().eq("status", "active");
        Criteria manualCriteria = Criteria.of().eq("ownerId", "user-1");
        when(recordService.describe(MODULE)).thenReturn(descriptor);
        when(recordService.mainEntity(MODULE)).thenReturn(operations);
        when(snapshotService.snapshot(MODULE)).thenReturn(new PlatformPageConfigSnapshot(
                MODULE, List.of(), List.of(), List.of(), List.of(template), List.of()));
        when(queryItemService.compile(eq("tpl-active"), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(templateCriteria);
        when(operations.queryCriteria(org.mockito.ArgumentMatchers.anyList())).thenReturn(manualCriteria);
        when(exportFacade.exportWorkbook(org.mockito.ArgumentMatchers.any(DynamicExportCommand.class)))
                .thenReturn(new byte[]{1, 2, 3});

        exportMvc.perform(post("/{moduleAlias}/export/data", MODULE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "queryTemplateId", "tpl-active",
                                "externalQueryValues", Map.of("owner", "user-1"),
                                "conditions", List.of(Map.of(
                                        "fieldName", "ownerId",
                                        "operator", "EQ",
                                        "values", List.of("user-1")
                                ))
                        ))))
                .andExpect(status().isOk());

        ArgumentCaptor<DynamicExportCommand> captor = ArgumentCaptor.forClass(DynamicExportCommand.class);
        verify(exportFacade).exportWorkbook(captor.capture());
        assertThat(captor.getValue().criteria()).isNotSameAs(templateCriteria);
        assertThat(captor.getValue().criteria()).isNotSameAs(manualCriteria);
        assertThat(captor.getValue().criteria().isEmpty()).isFalse();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> externalValues = ArgumentCaptor.forClass(Map.class);
        verify(queryItemService).compile(eq("tpl-active"), externalValues.capture());
        assertThat(externalValues.getValue()).containsEntry("owner", "user-1");
    }

    @Test
    void shouldRejectDataExportWhenModuleDoesNotSupportExchange() throws Exception {
        when(recordService.describe(MODULE)).thenReturn(descriptorWithoutExchange());

        mvc.perform(post("/{moduleAlias}/export/data", MODULE))
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

}
