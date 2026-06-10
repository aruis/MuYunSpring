package net.ximatai.muyun.spring.boot.dynamic;

import jakarta.servlet.http.HttpServletResponse;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicEntityOperations;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.exchange.exporter.DynamicExportCommand;
import net.ximatai.muyun.spring.platform.exchange.exporter.DynamicExportFacade;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigSnapshot;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigSnapshotService;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryItemService;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@RestController
@RequestMapping("/{moduleAlias:[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+}/export")
public class DynamicExportWebController {
    private final DynamicRecordService recordService;
    private final ActiveTenantVerifier activeTenantVerifier;
    private final DynamicExportFacade exportFacade;
    private final PlatformPageConfigSnapshotService pageConfigSnapshotService;
    private final PlatformQueryItemService queryItemService;

    public DynamicExportWebController(DynamicRecordService recordService,
                                      ActiveTenantVerifier activeTenantVerifier,
                                      DynamicExportFacade exportFacade) {
        this(recordService, activeTenantVerifier, exportFacade, (PlatformPageConfigSnapshotService) null, null);
    }

    @Autowired
    public DynamicExportWebController(DynamicRecordService recordService,
                                      ActiveTenantVerifier activeTenantVerifier,
                                      DynamicExportFacade exportFacade,
                                      ObjectProvider<PlatformPageConfigSnapshotService> pageConfigSnapshotServiceProvider,
                                      ObjectProvider<PlatformQueryItemService> queryItemServiceProvider) {
        this(recordService, activeTenantVerifier, exportFacade,
                pageConfigSnapshotServiceProvider == null ? null : pageConfigSnapshotServiceProvider.getIfAvailable(),
                queryItemServiceProvider == null ? null : queryItemServiceProvider.getIfAvailable());
    }

    public DynamicExportWebController(DynamicRecordService recordService,
                                      ActiveTenantVerifier activeTenantVerifier,
                                      DynamicExportFacade exportFacade,
                                      PlatformPageConfigSnapshotService pageConfigSnapshotService,
                                      PlatformQueryItemService queryItemService) {
        this.recordService = recordService;
        this.activeTenantVerifier = activeTenantVerifier;
        this.exportFacade = exportFacade;
        this.pageConfigSnapshotService = pageConfigSnapshotService;
        this.queryItemService = queryItemService;
    }

    @PostMapping("/data")
    @ActionEndpoint(PlatformAction.EXPORT)
    public void exportData(@PathVariable String moduleAlias,
                           @RequestBody(required = false) WebQueryRequest request,
                           HttpServletResponse response) {
        tenantScope(moduleAlias, () -> {
            DynamicModuleDescriptor descriptor = recordService.describe(moduleAlias);
            requireExchangeCapability(descriptor);
            byte[] bytes = exportFacade.exportWorkbook(exportCommand(moduleAlias, descriptor, request));
            writeXlsx(response, moduleAlias.replace('.', '_') + "-export.xlsx", bytes);
            return null;
        });
    }

    private DynamicExportCommand exportCommand(String moduleAlias,
                                               DynamicModuleDescriptor descriptor,
                                               WebQueryRequest request) {
        WebQueryRequest normalized = request == null ? new WebQueryRequest(null, List.of(), List.of()) : request;
        DynamicEntityOperations operations = recordService.mainEntity(moduleAlias);
        Criteria criteria = queryCriteria(moduleAlias, operations, normalized);
        PageRequest pageRequest = DynamicWebQueryMapper.page(normalized.pageOrDefault());
        Sort[] sorts = DynamicWebQueryMapper.sorts(normalized.sorts());
        return new DynamicExportCommand(descriptor, criteria, pageRequest, List.of(sorts));
    }

    private Criteria queryCriteria(String moduleAlias,
                                   DynamicEntityOperations operations,
                                   WebQueryRequest request) {
        Criteria templateCriteria = Criteria.of();
        if (request != null && hasText(request.queryTemplateId())) {
            requireLowCodeQueryServices();
            validateQueryTemplateBelongsToModule(moduleAlias, request.queryTemplateId());
            templateCriteria = queryItemService.compile(request.queryTemplateId(), request.externalQueryValues());
        }
        if (request == null || request.conditions().isEmpty()) {
            return templateCriteria;
        }
        Criteria manualCriteria = operations.queryCriteria(DynamicWebQueryMapper.queryConditions(request.conditions()));
        if (templateCriteria.isEmpty()) {
            return manualCriteria;
        }
        Criteria criteria = Criteria.of();
        criteria.andGroup(templateCriteria.getRoot());
        if (!manualCriteria.isEmpty()) {
            criteria.andGroup(manualCriteria.getRoot());
        }
        return criteria;
    }

    private void validateQueryTemplateBelongsToModule(String moduleAlias, String queryTemplateId) {
        PlatformPageConfigSnapshot snapshot = pageConfigSnapshotService.snapshot(moduleAlias);
        PlatformQueryTemplate template = snapshot.queryTemplates().stream()
                .filter(item -> Objects.equals(item.getId(), queryTemplateId))
                .findFirst()
                .orElseThrow(() -> new PlatformException("Query template is not enabled in module snapshot: "
                        + queryTemplateId));
        if (!Objects.equals(template.getModuleAlias(), moduleAlias)) {
            throw new PlatformException("Query template must belong to module: " + moduleAlias);
        }
    }

    private void requireLowCodeQueryServices() {
        if (pageConfigSnapshotService == null || queryItemService == null) {
            throw new PlatformException("dynamic low-code query services are not configured");
        }
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
            throw new PlatformException("dynamic export workbook write failed", ex);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
