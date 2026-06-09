package net.ximatai.muyun.spring.boot.dynamic;

import jakarta.servlet.http.HttpServletResponse;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.exchange.exporter.DynamicExportWorkbookPlanBuilder;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookPlan;
import net.ximatai.muyun.spring.platform.exchange.writer.ExcelWorkbookPlanWriter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.function.Supplier;

@RestController
@RequestMapping("/{moduleAlias:[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+}/export")
public class DynamicExportWebController {
    private final DynamicRecordService recordService;
    private final ActiveTenantVerifier activeTenantVerifier;
    private final DynamicExportWorkbookPlanBuilder planBuilder;
    private final ExcelWorkbookPlanWriter workbookWriter;

    public DynamicExportWebController(DynamicRecordService recordService,
                                      ActiveTenantVerifier activeTenantVerifier) {
        this(recordService, activeTenantVerifier,
                new DynamicExportWorkbookPlanBuilder(), new ExcelWorkbookPlanWriter());
    }

    DynamicExportWebController(DynamicRecordService recordService,
                               ActiveTenantVerifier activeTenantVerifier,
                               DynamicExportWorkbookPlanBuilder planBuilder,
                               ExcelWorkbookPlanWriter workbookWriter) {
        this.recordService = recordService;
        this.activeTenantVerifier = activeTenantVerifier;
        this.planBuilder = planBuilder;
        this.workbookWriter = workbookWriter;
    }

    @PostMapping("/template")
    @ActionEndpoint(PlatformAction.EXPORT)
    public void template(@PathVariable String moduleAlias, HttpServletResponse response) {
        tenantScope(moduleAlias, () -> {
            writeTemplate(moduleAlias, response);
            return null;
        });
    }

    private void writeTemplate(String moduleAlias, HttpServletResponse response) {
        DynamicModuleDescriptor descriptor = recordService.describe(moduleAlias);
        requireExchangeCapability(descriptor);
        ExcelWorkbookPlan plan = planBuilder.build(descriptor);
        byte[] bytes = workbookWriter.writeToBytes(plan);
        writeXlsx(response, moduleAlias.replace('.', '_') + "-template.xlsx", bytes);
    }

    private void requireExchangeCapability(DynamicModuleDescriptor descriptor) {
        DynamicEntityDescriptor mainEntity = descriptor.entities().stream()
                .filter(entity -> entity.entityAlias().equals(descriptor.mainEntityAlias()))
                .findFirst()
                .orElseThrow(() -> new PlatformException("dynamic module main entity not found: "
                        + descriptor.mainEntityAlias()));
        if (!mainEntity.capabilities().contains(EntityCapability.EXCHANGE.name())) {
            throw new PlatformException("dynamic entity does not support capability: EXCHANGE");
        }
    }

    private void writeXlsx(HttpServletResponse response, String fileName, byte[] bytes) {
        try {
            response.setContentType(DynamicImportWebController.XLSX_CONTENT_TYPE);
            response.setHeader("Content-Disposition", DynamicImportWebController.contentDisposition(fileName));
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition,X-Export-FileName");
            response.setHeader("X-Export-FileName", fileName);
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
        } catch (IOException ex) {
            throw new PlatformException("dynamic export template write failed", ex);
        }
    }

    private <T> T tenantScope(String moduleAlias, Supplier<T> action) {
        String tenantId = TenantContext.currentTenantId()
                .orElseThrow(() -> new PlatformException(moduleAlias + " requires tenant context"));
        activeTenantVerifier.verifyActiveTenant(tenantId);
        return action.get();
    }

    @ExceptionHandler({IllegalArgumentException.class, PlatformException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public DynamicWebError handleBadRequest(RuntimeException exception) {
        return DynamicWebError.badRequest(exception.getMessage());
    }
}
