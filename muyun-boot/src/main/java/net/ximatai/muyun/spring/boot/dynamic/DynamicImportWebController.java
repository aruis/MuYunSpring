package net.ximatai.muyun.spring.boot.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.platform.exchange.importer.BuildDynamicImportPlanCommand;
import net.ximatai.muyun.spring.platform.exchange.importer.DynamicImportCommand;
import net.ximatai.muyun.spring.platform.exchange.importer.DynamicImportErrorFileService;
import net.ximatai.muyun.spring.platform.exchange.importer.DynamicImportExecutionResult;
import net.ximatai.muyun.spring.platform.exchange.importer.DynamicImportFacade;
import net.ximatai.muyun.spring.platform.exchange.importer.DynamicImportParseResult;
import net.ximatai.muyun.spring.platform.exchange.importer.DynamicImportResult;
import net.ximatai.muyun.spring.platform.exchange.importer.ImportDuplicateStrategy;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Supplier;

@ApplicationScoped
@Path("/{moduleAlias:[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+}/import")
public class DynamicImportWebController {
    static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final DynamicRecordService recordService;
    private final DynamicImportFacade importFacade;
    private final DynamicImportErrorFileService errorFileService;
    private final TenantService activeTenantVerifier;
    private final ObjectMapper objectMapper;

    public DynamicImportWebController(DynamicRecordService recordService,
                                      DynamicImportFacade importFacade,
                                      DynamicImportErrorFileService errorFileService,
                                      TenantService activeTenantVerifier,
                                      ObjectMapper objectMapper) {
        this.recordService = recordService;
        this.importFacade = importFacade;
        this.errorFileService = errorFileService;
        this.activeTenantVerifier = activeTenantVerifier;
        this.objectMapper = objectMapper;
    }

    @POST
    @Path("/parse")
    @ActionEndpoint(PlatformAction.IMPORT)
    public DynamicImportParseResult parse(@PathParam("moduleAlias") String moduleAlias,
                                          @RestForm("file") FileUpload file) {
        return tenantScope(moduleAlias, () -> {
            DynamicModuleDescriptor descriptor = exchangeDescriptor(moduleAlias);
            return importFacade.parse(descriptor, bytes(file));
        });
    }

    @POST
    @Path("/execute")
    @ActionEndpoint(PlatformAction.IMPORT)
    public DynamicImportUploadResult execute(@PathParam("moduleAlias") String moduleAlias,
                                             @RestForm("command") String request,
                                             @RestForm("file") FileUpload file) {
        return tenantScope(moduleAlias, () -> {
            DynamicModuleDescriptor descriptor = exchangeDescriptor(moduleAlias);
            DynamicImportResult result = importFacade.importWorkbook(new DynamicImportCommand(
                    descriptor,
                    bytes(file),
                    buildCommand(moduleAlias, importRequest(request))
            ));
            return uploadResult(moduleAlias, result);
        });
    }

    @POST
    @Path("/error-file/{token}")
    @ActionEndpoint(PlatformAction.IMPORT)
    public Response downloadErrorFile(@PathParam("moduleAlias") String moduleAlias,
                                      @PathParam("token") String token) {
        return tenantScope(moduleAlias, () -> {
            exchangeDescriptor(moduleAlias);
            DynamicImportErrorFileService.ErrorFilePayload payload =
                    errorFileService.get(moduleAlias, currentTenantId(moduleAlias), token);
            if (payload == null) {
                throw new PlatformException("dynamic import error file token not found: " + token);
            }
            return xlsxResponse(payload.fileName(), payload.content());
        });
    }

    private DynamicModuleDescriptor exchangeDescriptor(String moduleAlias) {
        DynamicModuleDescriptor descriptor = recordService.describe(moduleAlias);
        DynamicEntityDescriptor mainEntity = descriptor.entities().stream()
                .filter(entity -> entity.entityAlias().equals(descriptor.mainEntityAlias()))
                .findFirst()
                .orElseThrow(() -> new PlatformException("dynamic module main entity not found: "
                        + descriptor.mainEntityAlias()));
        if (!mainEntity.capabilities().contains(EntityCapability.EXCHANGE.name())) {
            throw new PlatformException("dynamic entity does not support capability: EXCHANGE");
        }
        return descriptor;
    }

    private BuildDynamicImportPlanCommand buildCommand(String moduleAlias, DynamicImportExecuteRequest request) {
        if (request == null || request.mainSheet() == null) {
            throw new PlatformException("dynamic import execute requires mainSheet");
        }
        DynamicImportExecuteRequest.MainSheet mainSheet = request.mainSheet();
        return new BuildDynamicImportPlanCommand(
                moduleAlias,
                mainSheet.matchFieldName(),
                duplicateStrategy(mainSheet.duplicateStrategy()),
                request.childSheets().stream()
                        .map(child -> new BuildDynamicImportPlanCommand.ChildSheetCommand(
                                child.entityAlias(),
                                child.matchFieldName(),
                                duplicateStrategy(child.duplicateStrategy())
                        ))
                        .toList()
        );
    }

    private DynamicImportExecuteRequest importRequest(String request) {
        if (request == null || request.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(request, DynamicImportExecuteRequest.class);
        } catch (IOException ex) {
            throw new PlatformException("dynamic import command parse failed", ex);
        }
    }

    private ImportDuplicateStrategy duplicateStrategy(ImportDuplicateStrategy strategy) {
        return strategy == null ? ImportDuplicateStrategy.ERROR : strategy;
    }

    private DynamicImportUploadResult uploadResult(String moduleAlias, DynamicImportResult result) {
        DynamicImportExecutionResult execution = result.executionResult();
        int errorCount = execution.errorRows().size();
        int writtenCount = execution.created() + execution.updated();
        String errorFileName = null;
        String errorFileToken = null;
        byte[] errorWorkbookBytes = result.errorWorkbookBytes();
        if (errorWorkbookBytes != null && errorWorkbookBytes.length > 0) {
            errorFileName = moduleAlias.replace('.', '_') + "-import-errors.xlsx";
            errorFileToken = errorFileService.save(moduleAlias, currentTenantId(moduleAlias), errorFileName,
                    errorWorkbookBytes);
        }
        return new DynamicImportUploadResult(
                execution.created(),
                execution.updated(),
                execution.skipped(),
                errorCount,
                writtenCount > 0 && errorCount > 0,
                errorCount == 0 ? "import completed" : "import completed with errors",
                errorFileName,
                errorFileToken
        );
    }

    private String currentTenantId(String moduleAlias) {
        return TenantContext.currentTenantId()
                .orElseThrow(() -> new PlatformException(moduleAlias + " requires tenant context"));
    }

    private byte[] bytes(FileUpload file) {
        if (file == null || file.uploadedFile() == null) {
            throw new PlatformException("dynamic import file must not be empty");
        }
        try {
            return Files.readAllBytes(file.uploadedFile());
        } catch (IOException ex) {
            throw new PlatformException("dynamic import file read failed", ex);
        }
    }

    static String contentDisposition(String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + fileName.replace("\"", "_") + "\"; filename*=UTF-8''" + encoded;
    }

    private static Response xlsxResponse(String fileName, byte[] bytes) {
        return Response.ok(bytes, XLSX_CONTENT_TYPE)
                .header("Content-Disposition", contentDisposition(fileName))
                .header("Access-Control-Expose-Headers", "Content-Disposition,X-Import-FileName")
                .header("X-Import-FileName", fileName)
                .header("Content-Length", bytes.length)
                .build();
    }

    private <T> T tenantScope(String moduleAlias, Supplier<T> action) {
        activeTenantVerifier.verifyActiveTenant(currentTenantId(moduleAlias));
        return action.get();
    }

}
