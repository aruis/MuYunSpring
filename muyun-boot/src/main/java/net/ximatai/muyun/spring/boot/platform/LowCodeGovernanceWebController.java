package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.platform.config.LowCodeConfigHealthReport;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleConfigArchiveFacade;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleConfigArchiveResult;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleConfigVersion;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleHealthContext;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleHealthService;
import net.ximatai.muyun.spring.platform.config.LowCodeModulePackage;
import net.ximatai.muyun.spring.platform.config.LowCodeModulePackageExchangeService;
import net.ximatai.muyun.spring.platform.config.LowCodeModulePackageImportDraft;
import net.ximatai.muyun.spring.platform.config.LowCodeModulePackageImportService;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleTemplate;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleTemplateInstantiationRequest;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleTemplateService;
import net.ximatai.muyun.spring.platform.config.LowCodePackageDryRunResult;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = "platform.low_code_governance", title = "平台低代码治理")
@PlatformMenu(parent = PlatformMenuGroups.CONFIG, title = "低代码治理", order = 80)
@Path("/platform.low_code_governance")
public class LowCodeGovernanceWebController extends WebSupport<LowCodeModuleConfigArchiveFacade>
        implements SystemScope<LowCodeModuleConfigArchiveFacade> {
    private final LowCodeModuleHealthService healthService;
    private final LowCodeModulePackageExchangeService exchangeService;
    private final LowCodeModulePackageImportService importService;
    private final LowCodeModuleTemplateService templateService;

    public LowCodeGovernanceWebController(LowCodeModuleConfigArchiveFacade archiveFacade,
                                          LowCodeModuleHealthService healthService,
                                          LowCodeModulePackageExchangeService exchangeService,
                                          LowCodeModulePackageImportService importService,
                                          LowCodeModuleTemplateService templateService) {
        this.service = archiveFacade;
        this.healthService = healthService;
        this.exchangeService = exchangeService;
        this.importService = importService;
        this.templateService = templateService;
    }

    @POST
    @Path("/packages/health")
    @CustomActionEndpoint(value = "checkPackageHealth", title = "检查配置包健康度",
            level = PlatformActionLevel.LIST)
    public LowCodeConfigHealthReport checkPackageHealth(LowCodeModulePackage modulePackage) {
        return webScope(() -> healthService.check(LowCodeModuleHealthContext.ofPackage(modulePackage)));
    }

    @POST
    @Path("/packages/archive")
    @CustomActionEndpoint(value = "archivePackage", title = "归档配置包", level = PlatformActionLevel.LIST)
    public LowCodeModuleConfigArchiveResult archivePackage(ArchivePackageRequest request) {
        return webScope(() -> service().archive(request.modulePackage(), request.operatorId(), request.remark()));
    }

    @POST
    @Path("/modules/{moduleAlias}/versions/{versionId}/switch-current")
    @CustomActionEndpoint(value = "switchCurrentPackageVersion", title = "切换当前配置包版本",
            level = PlatformActionLevel.RECORD, recordIdPathVariable = "versionId")
    public LowCodeModuleConfigVersion switchCurrentPackageVersion(@PathParam("moduleAlias") String moduleAlias,
                                                            @PathParam("versionId") String versionId) {
        return webScope(() -> service().switchCurrentVersion(moduleAlias, versionId));
    }

    @GET
    @Path("/modules/{moduleAlias}/package")
    @CustomActionEndpoint(value = "exportCurrentPackage", title = "导出当前配置包",
            level = PlatformActionLevel.RECORD, recordIdPathVariable = "moduleAlias")
    public LowCodeModulePackage exportCurrentPackage(@PathParam("moduleAlias") String moduleAlias) {
        return webScope(() -> exchangeService.parsePackage(exchangeService.exportCurrentPackage(moduleAlias)));
    }

    @GET
    @Path("/versions/{versionId}/package")
    @CustomActionEndpoint(value = "exportVersionPackage", title = "导出指定版本配置包",
            level = PlatformActionLevel.RECORD, recordIdPathVariable = "versionId")
    public LowCodeModulePackage exportVersionPackage(@PathParam("versionId") String versionId) {
        return webScope(() -> exchangeService.parsePackage(exchangeService.exportVersionPackage(versionId)));
    }

    @POST
    @Path("/imports/dry-run")
    @CustomActionEndpoint(value = "dryRunImportPackage", title = "导入配置包预检",
            level = PlatformActionLevel.LIST)
    public LowCodePackageDryRunResult dryRunImportPackage(LowCodeModulePackage modulePackage) {
        return webScope(() -> exchangeService.dryRunImport(modulePackage));
    }

    @POST
    @Path("/imports/drafts")
    @CustomActionEndpoint(value = "prepareImportDraft", title = "准备导入草稿", level = PlatformActionLevel.LIST)
    public LowCodeModulePackageImportDraft prepareImportDraft(LowCodeModulePackage modulePackage) {
        return webScope(() -> importService.prepareDraft(modulePackage));
    }

    @POST
    @Path("/imports/drafts/archive")
    @CustomActionEndpoint(value = "archiveImportDraft", title = "归档导入草稿", level = PlatformActionLevel.LIST)
    public LowCodeModuleConfigArchiveResult archiveImportDraft(ArchiveImportDraftRequest request) {
        return webScope(() -> importService.archiveDraft(request.draft(), request.operatorId(), request.remark()));
    }

    @POST
    @Path("/templates/from-version")
    @CustomActionEndpoint(value = "createTemplateFromVersion", title = "从版本创建模板",
            level = PlatformActionLevel.LIST)
    public LowCodeModuleTemplate createTemplateFromVersion(CreateTemplateFromVersionRequest request) {
        return webScope(() -> templateService.createTemplateFromVersion(
                request.templateAlias(), request.title(), request.versionId()));
    }

    @POST
    @Path("/templates/instantiate")
    @CustomActionEndpoint(value = "instantiateTemplate", title = "实例化模板",
            level = PlatformActionLevel.LIST)
    public LowCodeModulePackage instantiateTemplate(InstantiateTemplateRequest request) {
        return webScope(() -> templateService.instantiate(request.template(), request.request()));
    }

    public record ArchivePackageRequest(
            LowCodeModulePackage modulePackage,
            String operatorId,
            String remark
    ) {
    }

    public record ArchiveImportDraftRequest(
            LowCodeModulePackageImportDraft draft,
            String operatorId,
            String remark
    ) {
    }

    public record CreateTemplateFromVersionRequest(
            String templateAlias,
            String title,
            String versionId
    ) {
    }

    public record InstantiateTemplateRequest(
            LowCodeModuleTemplate template,
            LowCodeModuleTemplateInstantiationRequest request
    ) {
    }
}
