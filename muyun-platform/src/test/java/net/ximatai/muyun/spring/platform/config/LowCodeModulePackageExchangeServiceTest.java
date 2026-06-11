package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LowCodeModulePackageExchangeServiceTest {
    private final LowCodeModuleConfigVersionService versionService =
            new LowCodeModuleConfigVersionService(new TestMemoryDao<>());
    private final LowCodeModuleHealthService healthService =
            new LowCodeModuleHealthService(List.of(new LowCodeModulePackageHealthChecker()));
    private final LowCodeModuleConfigPublishFacade publishFacade =
            new LowCodeModuleConfigPublishFacade(versionService, healthService);
    private final LowCodeModulePackageExchangeService exchangeService =
            new LowCodeModulePackageExchangeService(versionService, healthService);

    @Test
    void shouldExportCurrentAndSpecificVersionPackage() {
        LowCodeModuleConfigVersion version = publishFacade.publish(fullPackage("crm.contract"), "tester", null).version();

        String currentJson = exchangeService.exportCurrentPackage("crm.contract");
        String versionJson = exchangeService.exportVersionPackage(version.getId());

        assertThat(currentJson).contains("\"moduleAlias\":\"crm.contract\"");
        assertThat(versionJson).isEqualTo(currentJson);
        assertThat(exchangeService.parsePackage(currentJson).moduleAlias()).isEqualTo("crm.contract");
    }

    @Test
    void shouldBlockInvalidPackageOnDryRun() {
        LowCodeModulePackage invalid = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.PAGE_ONLY,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of("module", "crm.contract"))),
                null,
                null
        );

        LowCodePackageDryRunResult result = exchangeService.dryRunImport(invalid);

        assertThat(result.status()).isEqualTo(LowCodePackageDryRunStatus.BLOCKED);
        assertThat(result.healthReport().status()).isEqualTo(LowCodeConfigHealthStatus.FAIL);
    }

    @Test
    void shouldBlockPageOnlyPackageWhenTargetModuleDoesNotExist() {
        LowCodePackageDryRunResult result = exchangeService.dryRunImport(pageOnlyPackage("crm.contract"));

        assertThat(result.status()).isEqualTo(LowCodePackageDryRunStatus.BLOCKED);
        assertThat(result.conflicts()).extracting(LowCodePackageImportConflict::conflictType)
                .containsExactly(LowCodePackageConflictType.PAGE_ONLY_REQUIRES_EXISTING_MODULE);
    }

    @Test
    void shouldWarnWhenFullModulePackageTargetsExistingModule() {
        publishFacade.publish(fullPackage("crm.contract"), "tester", null);

        LowCodePackageDryRunResult result = exchangeService.dryRunImport(fullPackage("crm.contract"));

        assertThat(result.status()).isEqualTo(LowCodePackageDryRunStatus.WARN);
        assertThat(result.blocked()).isFalse();
        assertThat(result.conflicts().getFirst().severity()).isEqualTo(LowCodePackageConflictSeverity.WARN);
    }

    @Test
    void shouldBlockWhenRequiredDependencyIsMissing() {
        LowCodeModulePackageExchangeService service = new LowCodeModulePackageExchangeService(
                versionService, healthService, List.of(missingResolver(LowCodePackageDependencyType.MODULE)));
        LowCodeModulePackage modulePackage = fullPackage("crm.contract",
                List.of(LowCodePackageDependency.module("crm.customer")));

        LowCodePackageDryRunResult result = service.dryRunImport(modulePackage);

        assertThat(result.status()).isEqualTo(LowCodePackageDryRunStatus.BLOCKED);
        assertThat(result.conflicts()).extracting(LowCodePackageImportConflict::conflictType)
                .containsExactly(LowCodePackageConflictType.REQUIRED_DEPENDENCY_MISSING);
    }

    @Test
    void shouldWarnWhenOptionalDependencyIsMissing() {
        LowCodeModulePackageExchangeService service = new LowCodeModulePackageExchangeService(
                versionService, healthService, List.of(missingResolver(LowCodePackageDependencyType.MODULE)));
        LowCodeModulePackage modulePackage = fullPackage("crm.contract",
                List.of(new LowCodePackageDependency(LowCodePackageDependencyType.MODULE,
                        null, "crm.customer", null, false)));

        LowCodePackageDryRunResult result = service.dryRunImport(modulePackage);

        assertThat(result.status()).isEqualTo(LowCodePackageDryRunStatus.WARN);
        assertThat(result.conflicts()).extracting(LowCodePackageImportConflict::conflictType)
                .containsExactly(LowCodePackageConflictType.OPTIONAL_DEPENDENCY_MISSING);
    }

    @Test
    void shouldBlockRequiredDependencyWhenNoResolverIsAvailable() {
        LowCodeModulePackage modulePackage = fullPackage("crm.contract",
                List.of(LowCodePackageDependency.module("crm.customer")));

        LowCodePackageDryRunResult result = exchangeService.dryRunImport(modulePackage);

        assertThat(result.status()).isEqualTo(LowCodePackageDryRunStatus.BLOCKED);
        assertThat(result.conflicts()).extracting(LowCodePackageImportConflict::conflictType)
                .containsExactly(LowCodePackageConflictType.DEPENDENCY_RESOLVER_MISSING);
    }

    @Test
    void shouldBlockTemplatePackageWhenTargetModuleAlreadyExists() {
        publishFacade.publish(fullPackage("crm.contract"), "tester", null);

        LowCodePackageDryRunResult result = exchangeService.dryRunImport(templatePackage("crm.contract"));

        assertThat(result.status()).isEqualTo(LowCodePackageDryRunStatus.BLOCKED);
        assertThat(result.conflicts()).extracting(LowCodePackageImportConflict::conflictType)
                .containsExactly(LowCodePackageConflictType.TEMPLATE_TARGET_MODULE_EXISTS);
    }

    @Test
    void dryRunStatusShouldAlwaysBeDerivedFromHealthAndConflicts() {
        LowCodePackageDryRunResult result = new LowCodePackageDryRunResult(
                fullPackage("crm.contract"),
                LowCodePackageDryRunStatus.READY,
                LowCodeConfigHealthReport.of("crm.contract", List.of()),
                List.of(new LowCodePackageImportConflict(
                        LowCodePackageConflictType.PAGE_ONLY_REQUIRES_EXISTING_MODULE,
                        LowCodePackageConflictSeverity.ERROR,
                        "crm.contract",
                        null,
                        "missing target"
                ))
        );

        assertThat(result.status()).isEqualTo(LowCodePackageDryRunStatus.BLOCKED);
        assertThat(result.blocked()).isTrue();
    }

    @Test
    void shouldRejectBlankPackageJson() {
        assertThatThrownBy(() -> exchangeService.parsePackage(" "))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("low code module package json must not be blank");
    }

    private LowCodeModulePackage fullPackage(String moduleAlias) {
        return fullPackage(moduleAlias, List.of());
    }

    private LowCodeModulePackage fullPackage(String moduleAlias, List<LowCodePackageDependency> dependencies) {
        String applicationAlias = moduleAlias.substring(0, moduleAlias.indexOf('.'));
        return new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                applicationAlias,
                moduleAlias,
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of("module", moduleAlias))),
                new LowCodePackageDependencyManifest(dependencies),
                null
        );
    }

    private LowCodeModulePackage pageOnlyPackage(String moduleAlias) {
        String applicationAlias = moduleAlias.substring(0, moduleAlias.indexOf('.'));
        return new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.PAGE_ONLY,
                applicationAlias,
                moduleAlias,
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.PAGE,
                        Map.of("uiConfigs", List.of("list")))),
                null,
                null
        );
    }

    private LowCodeModulePackage templatePackage(String moduleAlias) {
        String applicationAlias = moduleAlias.substring(0, moduleAlias.indexOf('.'));
        return new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.TEMPLATE,
                applicationAlias,
                moduleAlias,
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of("module", moduleAlias))),
                null,
                null
        );
    }

    private LowCodePackageDependencyResolver missingResolver(LowCodePackageDependencyType type) {
        return new LowCodePackageDependencyResolver() {
            @Override
            public boolean supports(LowCodePackageDependencyType supportedType) {
                return supportedType == type;
            }

            @Override
            public boolean exists(LowCodePackageDependency dependency) {
                return false;
            }
        };
    }
}
