package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static net.ximatai.muyun.spring.platform.config.LowCodeConfigTestFixtures.completeDependencyResolver;
import static net.ximatai.muyun.spring.platform.config.LowCodeConfigTestFixtures.dependencyKeys;
import static net.ximatai.muyun.spring.platform.config.LowCodeConfigTestFixtures.dependencyKeysWithout;
import static net.ximatai.muyun.spring.platform.config.LowCodeConfigTestFixtures.salesContractPackage;

class M10LowCodeDemoBusinessAcceptanceTest {
    private final LowCodeModuleConfigVersionService sourceVersionService =
            new LowCodeModuleConfigVersionService(new TestMemoryDao<>());
    private final LowCodeModuleHealthService healthService =
            new LowCodeModuleHealthService(List.of(new LowCodeModulePackageHealthChecker()));
    private final LowCodeModuleConfigPublishFacade publishFacade =
            new LowCodeModuleConfigPublishFacade(sourceVersionService, healthService);
    private final LowCodeModulePackageExchangeService sourceExchangeService =
            exchangeService(sourceVersionService);
    private final LowCodeModuleTemplateService templateService =
            new LowCodeModuleTemplateService(sourceExchangeService);

    @Test
    void shouldAcceptSalesContractDemoAcrossM10GovernanceLoop() {
        LowCodeConfigTestFixtures.RecordingDependencyResolver dependencyResolver = completeDependencyResolver();
        LowCodeModulePackageExchangeService targetExchangeService =
                exchangeService(new LowCodeModuleConfigVersionService(new TestMemoryDao<>()), dependencyResolver);
        LowCodeModulePackage baseline = salesContractPackage("合同管理", "sales_contract", "draft");

        LowCodeModuleConfigPublishResult firstPublish = publishFacade.publish(baseline, "demo-admin", "demo baseline");

        assertThat(firstPublish.healthReport().status()).isEqualTo(LowCodeConfigHealthStatus.PASS);
        assertThat(firstPublish.version().getVersionNo()).isEqualTo(1);
        assertThat(firstPublish.version().getCurrentVersion()).isTrue();
        assertThat(firstPublish.version().getPackageHash()).hasSize(64);
        assertThat(firstPublish.version().getSummaryJson())
                .contains("\"mode\":\"MODULE_FULL\"")
                .contains("\"healthStatus\":\"PASS\"");

        String exported = sourceExchangeService.exportCurrentPackage("sales.contract");
        LowCodeModulePackage exportedPackage = sourceExchangeService.parsePackage(exported);
        assertThat(exportedPackage.applicationAlias()).isEqualTo("sales");
        assertThat(exportedPackage.moduleAlias()).isEqualTo("sales.contract");
        assertThat(exportedPackage.bundles()).extracting(LowCodeConfigBundle::type)
                .containsExactlyInAnyOrder(
                        LowCodePackageBundleType.METADATA,
                        LowCodePackageBundleType.PAGE,
                        LowCodePackageBundleType.INTERACTION,
                        LowCodePackageBundleType.ENTRY,
                        LowCodePackageBundleType.AUTOMATION
                );
        assertThat(exportedPackage.dependencyManifest().dependencies())
                .extracting(LowCodePackageDependency::type)
                .containsExactlyInAnyOrder(
                        LowCodePackageDependencyType.MODULE,
                        LowCodePackageDependencyType.DICTIONARY,
                        LowCodePackageDependencyType.ACTION,
                        LowCodePackageDependencyType.WORKFLOW,
                        LowCodePackageDependencyType.FILE_SERVICE,
                        LowCodePackageDependencyType.EXTERNAL
                );
        assertThat(exportedPackage.bundleMap().get(LowCodePackageBundleType.INTERACTION).content())
                .containsEntry("permissionActions", List.of("view", "create", "update", "submit", "generateInvoice"));

        assertThat(targetExchangeService.dryRunImport(exported).status()).isEqualTo(LowCodePackageDryRunStatus.READY);
        assertThat(dependencyResolver.resolvedKeys()).containsExactlyInAnyOrderElementsOf(dependencyKeys());

        LowCodeModuleConfigVersion secondVersion = publishFacade
                .publish(salesContractPackage("合同归档", "sales_contract_v2", "archived"), "demo-admin", "demo v2")
                .version();
        assertThat(secondVersion.getVersionNo()).isEqualTo(2);
        assertThat(sourceVersionService.currentVersion("sales.contract").getId()).isEqualTo(secondVersion.getId());
        assertThat(sourceExchangeService.exportCurrentPackage("sales.contract"))
                .contains("sales_contract_v2")
                .isNotEqualTo(exported);

        LowCodeModuleConfigVersion rolledBack = publishFacade.rollback("sales.contract", firstPublish.version().getId());
        assertThat(rolledBack.getId()).isEqualTo(firstPublish.version().getId());
        assertThat(sourceVersionService.currentVersion("sales.contract").getId()).isEqualTo(firstPublish.version().getId());
        assertThat(sourceExchangeService.exportCurrentPackage("sales.contract")).isEqualTo(exported);

        LowCodeModuleTemplate template = templateService.createTemplateFromVersion(
                "sales_contract_template", "合同模块样板", firstPublish.version().getId());
        LowCodeModulePackage renewalPackage = templateService.instantiate(template,
                new LowCodeModuleTemplateInstantiationRequest(
                        "sales",
                        "sales.contract.renewal",
                        "续签合同",
                        Map.of("tableName", "sales_contract_renewal")
                ));

        LowCodePackageDryRunResult renewalDryRun = targetExchangeService.dryRunImport(renewalPackage);
        assertThat(renewalDryRun.status()).isEqualTo(LowCodePackageDryRunStatus.READY);
        assertThat(renewalPackage.mode()).isEqualTo(LowCodePackageMode.MODULE_FULL);
        assertThat(renewalPackage.moduleAlias()).isEqualTo("sales.contract.renewal");
        assertThat(renewalPackage.bundleMap().get(LowCodePackageBundleType.METADATA).content())
                .containsEntry("module", "sales.contract.renewal")
                .containsEntry("title", "续签合同")
                .containsEntry("tableName", "sales_contract_renewal");
    }

    @Test
    void shouldBlockDemoMigrationWhenRequiredDependencyIsMissing() {
        LowCodeModulePackage contractPackage = salesContractPackage("合同管理", "sales_contract", "draft");
        LowCodeConfigTestFixtures.RecordingDependencyResolver dependencyResolver =
                new LowCodeConfigTestFixtures.RecordingDependencyResolver(
                Set.of(
                        LowCodePackageDependencyType.MODULE,
                        LowCodePackageDependencyType.DICTIONARY,
                        LowCodePackageDependencyType.ACTION,
                        LowCodePackageDependencyType.WORKFLOW,
                        LowCodePackageDependencyType.FILE_SERVICE,
                        LowCodePackageDependencyType.EXTERNAL
                ),
                dependencyKeysWithout("MODULE:sales.customer")
        );
        LowCodeModulePackageExchangeService strictExchangeService = new LowCodeModulePackageExchangeService(
                new LowCodeModuleConfigVersionService(new TestMemoryDao<>()),
                healthService,
                List.of(dependencyResolver)
        );

        LowCodePackageDryRunResult result = strictExchangeService.dryRunImport(contractPackage);

        assertThat(result.status()).isEqualTo(LowCodePackageDryRunStatus.BLOCKED);
        assertThat(result.conflicts()).extracting(LowCodePackageImportConflict::conflictType)
                .contains(LowCodePackageConflictType.REQUIRED_DEPENDENCY_MISSING);
    }

    private LowCodeModulePackageExchangeService exchangeService(LowCodeModuleConfigVersionService versionService) {
        return exchangeService(versionService, completeDependencyResolver());
    }

    private LowCodeModulePackageExchangeService exchangeService(LowCodeModuleConfigVersionService versionService,
                                                               LowCodeConfigTestFixtures.RecordingDependencyResolver dependencyResolver) {
        return new LowCodeModulePackageExchangeService(
                versionService,
                healthService,
                List.of(dependencyResolver)
        );
    }
}
