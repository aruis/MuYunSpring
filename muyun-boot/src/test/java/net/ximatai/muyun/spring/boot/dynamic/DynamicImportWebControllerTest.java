package net.ximatai.muyun.spring.boot.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.platform.exchange.importer.BuildDynamicImportPlanCommand;
import net.ximatai.muyun.spring.platform.exchange.importer.DynamicImportCommand;
import net.ximatai.muyun.spring.platform.exchange.importer.DynamicImportErrorFileService;
import net.ximatai.muyun.spring.platform.exchange.importer.DynamicImportExecutionResult;
import net.ximatai.muyun.spring.platform.exchange.importer.DynamicImportFacade;
import net.ximatai.muyun.spring.platform.exchange.importer.DynamicImportParseResult;
import net.ximatai.muyun.spring.platform.exchange.importer.DynamicImportPlan;
import net.ximatai.muyun.spring.platform.exchange.importer.DynamicImportResult;
import net.ximatai.muyun.spring.platform.exchange.importer.GroupedWorkbook;
import net.ximatai.muyun.spring.platform.exchange.importer.ImportDuplicateStrategy;
import net.ximatai.muyun.spring.platform.exchange.importer.ImportErrorRow;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicImportWebControllerTest {
    private static final String MODULE = "sales.order";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private DynamicRecordService recordService;
    private DynamicImportFacade importFacade;
    private DynamicImportErrorFileService errorFileService;
    private TenantService activeTenantVerifier;
    private DynamicImportWebController controller;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant_a");
        recordService = mock(DynamicRecordService.class);
        importFacade = mock(DynamicImportFacade.class);
        errorFileService = new DynamicImportErrorFileService();
        activeTenantVerifier = mock(TenantService.class);
        controller = new DynamicImportWebController(
                recordService, importFacade, errorFileService, activeTenantVerifier, objectMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldParseMultipartWorkbookThroughFacade() throws Exception {
        DynamicModuleDescriptor descriptor = descriptor();
        when(recordService.describe(MODULE)).thenReturn(descriptor);
        when(importFacade.parse(descriptor, new byte[]{1, 2, 3})).thenReturn(new DynamicImportParseResult(
                MODULE,
                "order",
                "Order",
                List.of(new DynamicImportParseResult.Sheet(
                        "Order",
                        "Order",
                        "order",
                        true,
                        1,
                        List.of(new DynamicImportParseResult.Field("orderNo", "Order No", false, true))
                ))
        ));

        DynamicImportParseResult response = controller.parse(MODULE, upload(new byte[]{1, 2, 3}));

        assertThat(response.moduleAlias()).isEqualTo(MODULE);
        assertThat(response.mainEntityAlias()).isEqualTo("order");
        assertThat(response.mainSheetName()).isEqualTo("Order");
        assertThat(response.sheets().getFirst().fields().getFirst().matchKeyCandidate()).isTrue();

        verify(recordService).describe(MODULE);
        verify(activeTenantVerifier).verifyActiveTenant("tenant_a");
    }

    @Test
    void shouldRejectParseWhenModuleDoesNotSupportExchange() throws Exception {
        when(recordService.describe(MODULE)).thenReturn(descriptorWithoutExchange());

        assertThatThrownBy(() -> controller.parse(MODULE, upload(new byte[]{1, 2, 3})))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("dynamic entity does not support capability");
    }

    @Test
    void shouldExecuteImportAndReturnErrorFileToken() throws Exception {
        DynamicModuleDescriptor descriptor = descriptor();
        when(recordService.describe(MODULE)).thenReturn(descriptor);
        when(importFacade.importWorkbook(any(DynamicImportCommand.class))).thenReturn(importResultWithErrors());

        String command = objectMapper.writeValueAsString(Map.of(
                        "mainSheet", Map.of(
                                "matchFieldName", "orderNo",
                                "duplicateStrategy", "OVERWRITE"
                        ),
                        "childSheets", List.of(Map.of(
                                "entityAlias", "orderLine",
                                "matchFieldName", "sku",
                                "duplicateStrategy", "SKIP"
                        ))
                ));

        DynamicImportUploadResult response = controller.execute(MODULE, command, upload(new byte[]{4, 5, 6}));

        assertThat(response.created()).isEqualTo(1);
        assertThat(response.updated()).isEqualTo(2);
        assertThat(response.skipped()).isEqualTo(3);
        assertThat(response.errorCount()).isEqualTo(1);
        assertThat(response.partialSuccess()).isTrue();
        assertThat(response.errorFileName()).isEqualTo("sales_order-import-errors.xlsx");
        assertThat(response.errorFileToken()).isNotBlank();

        ArgumentCaptor<DynamicImportCommand> captor = ArgumentCaptor.forClass(DynamicImportCommand.class);
        verify(importFacade).importWorkbook(captor.capture());
        assertThat(captor.getValue().descriptor()).isSameAs(descriptor);
        assertThat(captor.getValue().excelBytes()).containsExactly(4, 5, 6);
        BuildDynamicImportPlanCommand buildCommand = captor.getValue().buildPlanCommand();
        assertThat(buildCommand.mainMatchFieldName()).isEqualTo("orderNo");
        assertThat(buildCommand.mainDuplicateStrategy()).isEqualTo(ImportDuplicateStrategy.OVERWRITE);
        assertThat(buildCommand.childSheets().getFirst().entityAlias()).isEqualTo("orderLine");
        assertThat(buildCommand.childSheets().getFirst().duplicateStrategy()).isEqualTo(ImportDuplicateStrategy.SKIP);
    }

    @Test
    void shouldTreatSkippedRowsAsHandledWhenErrorsAlsoExist() throws Exception {
        DynamicModuleDescriptor descriptor = descriptor();
        when(recordService.describe(MODULE)).thenReturn(descriptor);
        when(importFacade.importWorkbook(any(DynamicImportCommand.class))).thenReturn(importResultWithOnlySkippedAndErrors());

        String command = objectMapper.writeValueAsString(Map.of(
                        "mainSheet", Map.of("matchFieldName", "orderNo")
                ));

        DynamicImportUploadResult response = controller.execute(MODULE, command, upload(new byte[]{4, 5, 6}));

        assertThat(response.created()).isZero();
        assertThat(response.updated()).isZero();
        assertThat(response.skipped()).isEqualTo(2);
        assertThat(response.errorCount()).isEqualTo(1);
        assertThat(response.partialSuccess()).isFalse();
    }

    @Test
    void shouldDownloadErrorFileByToken() throws Exception {
        String token = errorFileService.save(MODULE, "tenant_a", "errors.xlsx", new byte[]{9, 8, 7});
        when(recordService.describe(MODULE)).thenReturn(descriptor());


        Response response = controller.downloadErrorFile(MODULE, token);

        assertThat(response.getHeaderString("X-Import-FileName")).isEqualTo("errors.xlsx");
        assertThat(response.getHeaderString("Access-Control-Expose-Headers"))
                .isEqualTo("Content-Disposition,X-Import-FileName");
        assertThat(response.getHeaderString("Content-Length")).isEqualTo("3");
        assertThat(response.getMediaType().toString()).isEqualTo(DynamicImportWebController.XLSX_CONTENT_TYPE);
        assertThat((byte[]) response.getEntity()).containsExactly(9, 8, 7);
    }

    @Test
    void shouldRejectErrorFileTokenFromAnotherModule() throws Exception {
        String token = errorFileService.save("crm.customer", "tenant_a", "errors.xlsx", new byte[]{9, 8, 7});
        when(recordService.describe(MODULE)).thenReturn(descriptor());

        assertThatThrownBy(() -> controller.downloadErrorFile(MODULE, token))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("dynamic import error file token not found");
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

    private DynamicImportResult importResultWithErrors() {
        DynamicImportPlan plan = new DynamicImportPlan(MODULE, null, List.of(
                new DynamicImportPlan.SheetPlan(
                        "Order",
                        "order",
                        "Order",
                        true,
                        "orderNo",
                        ImportDuplicateStrategy.ERROR,
                        List.of(new DynamicImportPlan.FieldPlan("order", "orderNo", "Order No",
                                false, true, false))
                )
        ));
        DynamicImportExecutionResult execution = new DynamicImportExecutionResult(
                1,
                2,
                3,
                List.of(new ImportErrorRow("order", new LinkedHashMap<>(), "invalid", null)),
                Map.of()
        );
        return new DynamicImportResult(plan, new GroupedWorkbook(new LinkedHashMap<>(), List.of()),
                execution, new byte[]{7, 8, 9});
    }

    private DynamicImportResult importResultWithOnlySkippedAndErrors() {
        DynamicImportPlan plan = new DynamicImportPlan(MODULE, null, List.of(
                new DynamicImportPlan.SheetPlan(
                        "Order",
                        "order",
                        "Order",
                        true,
                        "orderNo",
                        ImportDuplicateStrategy.ERROR,
                        List.of(new DynamicImportPlan.FieldPlan("order", "orderNo", "Order No",
                                false, true, false))
                )
        ));
        DynamicImportExecutionResult execution = new DynamicImportExecutionResult(
                0,
                0,
                2,
                List.of(new ImportErrorRow("order", new LinkedHashMap<>(), "invalid", null)),
                Map.of()
        );
        return new DynamicImportResult(plan, new GroupedWorkbook(new LinkedHashMap<>(), List.of()),
                execution, new byte[]{7, 8, 9});
    }

    private FileUpload upload(byte[] bytes) throws IOException {
        Path path = Files.createTempFile("dynamic-import-", ".xlsx");
        Files.write(path, bytes);
        return new TestFileUpload(path, bytes.length);
    }

    private record TestFileUpload(Path uploadedFile, long size) implements FileUpload {
        @Override
        public String name() {
            return "file";
        }

        @Override
        public Path filePath() {
            return uploadedFile;
        }

        @Override
        public String fileName() {
            return "order.xlsx";
        }

        @Override
        public String contentType() {
            return DynamicImportWebController.XLSX_CONTENT_TYPE;
        }

        @Override
        public String charSet() {
            return null;
        }

        @Override
        public MultivaluedMap<String, String> getHeaders() {
            return new MultivaluedHashMap<>();
        }
    }

}
