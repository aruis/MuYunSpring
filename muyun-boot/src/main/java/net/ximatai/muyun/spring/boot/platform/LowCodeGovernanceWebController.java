package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.platform.config.LowCodeConfigHealthReport;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleConfigPublishFacade;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleConfigPublishResult;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleConfigVersion;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleHealthContext;
import net.ximatai.muyun.spring.platform.config.LowCodeModuleHealthService;
import net.ximatai.muyun.spring.platform.config.LowCodeModulePackage;
import net.ximatai.muyun.spring.platform.config.LowCodeModulePackageExchangeService;
import net.ximatai.muyun.spring.platform.config.LowCodeModulePackageImportDraft;
import net.ximatai.muyun.spring.platform.config.LowCodeModulePackageImportService;
import net.ximatai.muyun.spring.platform.config.LowCodePackageDryRunResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PlatformStaticModule(application = "platform", alias = "platform.low_code_governance", title = "平台低代码治理")
@RequestMapping({"/platform.low_code_governance", "/platform/low-code-governance"})
public class LowCodeGovernanceWebController extends WebSupport<LowCodeModuleConfigPublishFacade>
        implements SystemScope<LowCodeModuleConfigPublishFacade> {
    private final LowCodeModuleHealthService healthService;
    private final LowCodeModulePackageExchangeService exchangeService;
    private final LowCodeModulePackageImportService importService;

    public LowCodeGovernanceWebController(LowCodeModuleConfigPublishFacade publishFacade,
                                          LowCodeModuleHealthService healthService,
                                          LowCodeModulePackageExchangeService exchangeService,
                                          LowCodeModulePackageImportService importService) {
        this.service = publishFacade;
        this.healthService = healthService;
        this.exchangeService = exchangeService;
        this.importService = importService;
    }

    @PostMapping("/packages/health")
    @CustomActionEndpoint(value = "checkPackageHealth", title = "检查配置包健康度",
            level = PlatformActionLevel.LIST)
    public LowCodeConfigHealthReport checkPackageHealth(@RequestBody LowCodeModulePackage modulePackage) {
        return webScope(() -> healthService.check(LowCodeModuleHealthContext.ofPackage(modulePackage)));
    }

    @PostMapping("/packages/publish")
    @CustomActionEndpoint(value = "publishPackage", title = "发布配置包", level = PlatformActionLevel.LIST)
    public LowCodeModuleConfigPublishResult publishPackage(@RequestBody PublishPackageRequest request) {
        return webScope(() -> service().publish(request.modulePackage(), request.operatorId(), request.remark()));
    }

    @PostMapping("/modules/{moduleAlias}/versions/{versionId}/rollback")
    @CustomActionEndpoint(value = "rollbackPackageVersion", title = "回滚配置包版本",
            level = PlatformActionLevel.RECORD, recordIdPathVariable = "versionId")
    public LowCodeModuleConfigVersion rollbackPackageVersion(@PathVariable String moduleAlias,
                                                            @PathVariable String versionId) {
        return webScope(() -> service().rollback(moduleAlias, versionId));
    }

    @GetMapping("/modules/{moduleAlias}/package")
    @CustomActionEndpoint(value = "exportCurrentPackage", title = "导出当前配置包",
            level = PlatformActionLevel.RECORD, recordIdPathVariable = "moduleAlias")
    public LowCodeModulePackage exportCurrentPackage(@PathVariable String moduleAlias) {
        return webScope(() -> exchangeService.parsePackage(exchangeService.exportCurrentPackage(moduleAlias)));
    }

    @GetMapping("/versions/{versionId}/package")
    @CustomActionEndpoint(value = "exportVersionPackage", title = "导出指定版本配置包",
            level = PlatformActionLevel.RECORD, recordIdPathVariable = "versionId")
    public LowCodeModulePackage exportVersionPackage(@PathVariable String versionId) {
        return webScope(() -> exchangeService.parsePackage(exchangeService.exportVersionPackage(versionId)));
    }

    @PostMapping("/imports/dry-run")
    @CustomActionEndpoint(value = "dryRunImportPackage", title = "导入配置包预检",
            level = PlatformActionLevel.LIST)
    public LowCodePackageDryRunResult dryRunImportPackage(@RequestBody LowCodeModulePackage modulePackage) {
        return webScope(() -> exchangeService.dryRunImport(modulePackage));
    }

    @PostMapping("/imports/drafts")
    @CustomActionEndpoint(value = "prepareImportDraft", title = "准备导入草稿", level = PlatformActionLevel.LIST)
    public LowCodeModulePackageImportDraft prepareImportDraft(@RequestBody LowCodeModulePackage modulePackage) {
        return webScope(() -> importService.prepareDraft(modulePackage));
    }

    @PostMapping("/imports/drafts/publish")
    @CustomActionEndpoint(value = "publishImportDraft", title = "发布导入草稿", level = PlatformActionLevel.LIST)
    public LowCodeModuleConfigPublishResult publishImportDraft(@RequestBody PublishImportDraftRequest request) {
        return webScope(() -> importService.publishDraft(request.draft(), request.operatorId(), request.remark()));
    }

    public record PublishPackageRequest(
            LowCodeModulePackage modulePackage,
            String operatorId,
            String remark
    ) {
    }

    public record PublishImportDraftRequest(
            LowCodeModulePackageImportDraft draft,
            String operatorId,
            String remark
    ) {
    }
}
