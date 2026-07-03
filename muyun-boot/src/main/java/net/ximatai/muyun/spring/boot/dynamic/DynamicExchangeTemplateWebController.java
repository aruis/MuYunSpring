package net.ximatai.muyun.spring.boot.dynamic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookPlan;
import net.ximatai.muyun.spring.platform.exchange.template.DynamicExchangeTemplateOptions;
import net.ximatai.muyun.spring.platform.exchange.template.DynamicExchangeTemplatePlanBuilder;
import net.ximatai.muyun.spring.platform.exchange.template.DynamicRecordReferenceDropdownResolver;
import net.ximatai.muyun.spring.platform.exchange.writer.ExcelWorkbookPlanWriter;

import java.util.function.Supplier;

@ApplicationScoped
@Path("/{moduleAlias:[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+}/exchange")
public class DynamicExchangeTemplateWebController {
    private final DynamicRecordService recordService;
    private final TenantService activeTenantVerifier;
    private final DynamicExchangeTemplatePlanBuilder templatePlanBuilder;
    private final ExcelWorkbookPlanWriter workbookWriter;

    @Inject
    public DynamicExchangeTemplateWebController(DynamicRecordService recordService,
                                                TenantService activeTenantVerifier,
                                                OptionSourceRegistry optionSourceRegistry) {
        this(recordService, activeTenantVerifier,
                new DynamicExchangeTemplatePlanBuilder(optionSourceRegistry,
                        new DynamicRecordReferenceDropdownResolver(recordService)),
                new ExcelWorkbookPlanWriter());
    }

    DynamicExchangeTemplateWebController(DynamicRecordService recordService,
                                         TenantService activeTenantVerifier,
                                         DynamicExchangeTemplatePlanBuilder templatePlanBuilder,
                                         ExcelWorkbookPlanWriter workbookWriter) {
        this.recordService = recordService;
        this.activeTenantVerifier = activeTenantVerifier;
        this.templatePlanBuilder = templatePlanBuilder;
        this.workbookWriter = workbookWriter;
    }

    @POST
    @Path("/template")
    @ActionEndpoint(PlatformAction.IMPORT)
    public Response template(@PathParam("moduleAlias") String moduleAlias,
                             DynamicExchangeTemplateRequest request) {
        return tenantScope(moduleAlias, () -> templateResponse(moduleAlias, request));
    }

    private Response templateResponse(String moduleAlias,
                                      DynamicExchangeTemplateRequest request) {
        DynamicModuleDescriptor descriptor = recordService.describe(moduleAlias);
        requireExchangeCapability(descriptor);
        ExcelWorkbookPlan plan = templatePlanBuilder.build(descriptor, templateOptions(request));
        byte[] bytes = workbookWriter.writeToBytes(plan);
        String fileName = moduleAlias.replace('.', '_') + "-exchange-template.xlsx";
        return Response.ok(bytes, DynamicImportWebController.XLSX_CONTENT_TYPE)
                .header("Content-Disposition", DynamicImportWebController.contentDisposition(fileName))
                .header("Access-Control-Expose-Headers", "Content-Disposition,X-Exchange-FileName")
                .header("X-Exchange-FileName", fileName)
                .header("Content-Length", bytes.length)
                .build();
    }

    private DynamicExchangeTemplateOptions templateOptions(DynamicExchangeTemplateRequest request) {
        if (request == null) {
            return DynamicExchangeTemplateOptions.DEFAULT;
        }
        return DynamicExchangeTemplateOptions.of(
                request.disabledReferenceDropdownFields(),
                request.referenceDropdownLimit()
        );
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

    private <T> T tenantScope(String moduleAlias, Supplier<T> action) {
        String tenantId = TenantContext.currentTenantId()
                .orElseThrow(() -> new PlatformException(moduleAlias + " requires tenant context"));
        activeTenantVerifier.verifyActiveTenant(tenantId);
        return action.get();
    }

}
