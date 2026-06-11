package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static net.ximatai.muyun.spring.platform.config.LowCodeConfigTestFixtures.fullPackage;
import static net.ximatai.muyun.spring.platform.config.LowCodeConfigTestFixtures.fullPackageWithPageBundle;
import static net.ximatai.muyun.spring.platform.config.LowCodeConfigTestFixtures.pageOnlyPackage;

class LowCodeModuleTemplateServiceTest {
    private final LowCodeModuleConfigVersionService versionService =
            new LowCodeModuleConfigVersionService(new TestMemoryDao<>());
    private final LowCodeModuleHealthService healthService =
            new LowCodeModuleHealthService(List.of(new LowCodeModulePackageHealthChecker()));
    private final LowCodeModuleConfigPublishFacade publishFacade =
            new LowCodeModuleConfigPublishFacade(versionService, healthService);
    private final LowCodeModulePackageExchangeService exchangeService =
            new LowCodeModulePackageExchangeService(versionService, healthService);
    private final LowCodeModuleTemplateService templateService =
            new LowCodeModuleTemplateService(exchangeService);

    @Test
    void shouldCreateTemplateFromPublishedVersionAndInstantiateNewModulePackage() {
        LowCodeModuleConfigVersion version = publishFacade.publish(fullPackageWithPageBundle("crm.contract"), "tester", null).version();

        LowCodeModuleTemplate template = templateService.createTemplateFromVersion(
                "contract_template", "Contract Template", version.getId());
        LowCodeModulePackage instantiated = templateService.instantiate(template,
                new LowCodeModuleTemplateInstantiationRequest(
                        "sales",
                        "sales.contract",
                        "Sales Contract",
                        Map.of("tableName", "sales_contract")
                ));

        assertThat(template.basePackage().mode()).isEqualTo(LowCodePackageMode.TEMPLATE);
        assertThat(instantiated.mode()).isEqualTo(LowCodePackageMode.MODULE_FULL);
        assertThat(instantiated.applicationAlias()).isEqualTo("sales");
        assertThat(instantiated.moduleAlias()).isEqualTo("sales.contract");
        assertThat(instantiated.bundles().getFirst().content())
                .containsEntry("module", "sales.contract")
                .containsEntry("title", "Sales Contract")
                .containsEntry("tableName", "sales_contract");
        assertThat(instantiated.bundles().get(1).content())
                .containsEntry("moduleAlias", "sales.contract")
                .doesNotContainKeys("title", "tableName");
    }

    @Test
    void instantiatedPackageShouldPassDryRunForNewModule() {
        LowCodeModuleConfigVersion version = publishFacade.publish(fullPackage("crm.contract"), "tester", null).version();
        LowCodeModuleTemplate template = templateService.createTemplateFromVersion(
                "contract_template", "Contract Template", version.getId());

        LowCodeModulePackage instantiated = templateService.instantiate(template,
                new LowCodeModuleTemplateInstantiationRequest("sales", "sales.contract", null, Map.of()));
        LowCodePackageDryRunResult result = exchangeService.dryRunImport(instantiated);

        assertThat(result.status()).isEqualTo(LowCodePackageDryRunStatus.READY);
    }

    @Test
    void shouldRejectTemplateTargetOutsideApplication() {
        LowCodeModuleConfigVersion version = publishFacade.publish(fullPackage("crm.contract"), "tester", null).version();
        LowCodeModuleTemplate template = templateService.createTemplateFromVersion(
                "contract_template", "Contract Template", version.getId());

        assertThatThrownBy(() -> templateService.instantiate(template,
                new LowCodeModuleTemplateInstantiationRequest("sales", "crm.contract", null, Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moduleAlias must start with applicationAlias");
    }

    @Test
    void shouldRejectReservedTemplateParameters() {
        assertThatThrownBy(() -> new LowCodeModuleTemplateInstantiationRequest(
                "sales", "sales.contract", null, Map.of("moduleAlias", "crm.contract")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("template parameter is reserved: moduleAlias");
    }

    @Test
    void shouldKeepExternalDependenciesUnchangedWhenInstantiating() {
        LowCodeModulePackage source = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.TEMPLATE,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of("module", "crm.contract", "title", "Contract"))),
                new LowCodePackageDependencyManifest(List.of(
                        LowCodePackageDependency.action("crm.contract", "submit")
                )),
                null
        );
        LowCodeModuleTemplate template = new LowCodeModuleTemplate("contract_template", "Contract Template", source);

        LowCodeModulePackage instantiated = templateService.instantiate(template,
                new LowCodeModuleTemplateInstantiationRequest("sales", "sales.contract", null, Map.of()));

        assertThat(instantiated.dependencyManifest().dependencies().getFirst().moduleAlias())
                .isEqualTo("crm.contract");
    }

    @Test
    void shouldRejectPageOnlyVersionAsTemplateSource() {
        LowCodeModuleConfigVersion version = publishFacade.publish(pageOnlyPackage("crm.contract"), "tester", null).version();

        assertThatThrownBy(() -> templateService.createTemplateFromVersion(
                "contract_template", "Contract Template", version.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("template source package must be MODULE_FULL with METADATA bundle");
    }

}
