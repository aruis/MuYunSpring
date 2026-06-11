package net.ximatai.muyun.spring.platform.config;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LowCodeModulePackageValidatorTest {
    private final LowCodeModulePackageValidator validator = new LowCodeModulePackageValidator();

    @Test
    void shouldAcceptFullModulePackageWithLayeredBundles() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(
                        LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                                Map.of("module", "crm.contract")),
                        LowCodeConfigBundle.included(LowCodePackageBundleType.PAGE,
                                Map.of("uiConfigs", List.of("list", "form"))),
                        LowCodeConfigBundle.included(LowCodePackageBundleType.INTERACTION,
                                Map.of("taskDefinitions", List.of("profile-ready"))),
                        LowCodeConfigBundle.omitted(LowCodePackageBundleType.AUTOMATION)
                ),
                new LowCodePackageDependencyManifest(List.of(
                        LowCodePackageDependency.module("crm.customer"),
                        LowCodePackageDependency.action("crm.contract", "localEdit"),
                        LowCodePackageDependency.dictionary("crm", "contract_status")
                )),
                new LowCodePackagePublishManifest("m10.v1", "dev", "version-1", "tester",
                        Instant.parse("2026-06-11T00:00:00Z"), "baseline")
        );

        validator.validate(modulePackage);

        assertThat(modulePackage.applicationAlias()).isEqualTo("crm");
        assertThat(modulePackage.moduleAlias()).isEqualTo("crm.contract");
        assertThat(modulePackage.includes(LowCodePackageBundleType.METADATA)).isTrue();
        assertThat(modulePackage.includes(LowCodePackageBundleType.AUTOMATION)).isFalse();
    }

    @Test
    void shouldRejectModuleAliasOutsideApplication() {
        assertThatThrownBy(() -> new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "sales.contract",
                List.of(),
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moduleAlias must start with applicationAlias");
    }

    @Test
    void shouldRejectPageOnlyPackageWithMetadataBundle() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.PAGE_ONLY,
                "crm",
                "crm.contract",
                List.of(
                        LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                                Map.of("module", "crm.contract")),
                        LowCodeConfigBundle.included(LowCodePackageBundleType.PAGE,
                                Map.of("uiConfigs", List.of("list")))
                ),
                null,
                null
        );

        assertThatThrownBy(() -> validator.validate(modulePackage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PAGE_ONLY package cannot include METADATA bundle");
    }

    @Test
    void shouldRejectIncludedEmptyBundle() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA, Map.of())),
                null,
                null
        );

        assertThatThrownBy(() -> validator.validate(modulePackage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("included package bundle must not be empty: METADATA");
    }

    @Test
    void shouldRejectPageOnlyPackageWithoutPageBundle() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.PAGE_ONLY,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.ENTRY,
                        Map.of("menus", List.of("contract-menu")))),
                null,
                null
        );

        assertThatThrownBy(() -> validator.validate(modulePackage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PAGE_ONLY package must include PAGE bundle");
    }

    @Test
    void shouldRejectDuplicatedBundleType() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(
                        LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                                Map.of("module", "crm.contract")),
                        LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                                Map.of("fields", List.of("name")))
                ),
                null,
                null
        );

        assertThatThrownBy(() -> validator.validate(modulePackage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicated package bundle: METADATA");
        assertThatThrownBy(modulePackage::bundleMap)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicated package bundle: METADATA");
    }

    @Test
    void shouldRejectDependencyWithoutRequiredIdentity() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of("module", "crm.contract"))),
                new LowCodePackageDependencyManifest(List.of(
                        new LowCodePackageDependency(LowCodePackageDependencyType.DICTIONARY,
                                "crm", null, null, true)
                )),
                null
        );

        assertThatThrownBy(() -> validator.validate(modulePackage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DICTIONARY dependency alias must not be blank");
    }

    @Test
    void shouldRejectActionDependencyWithoutActionCode() {
        LowCodeModulePackage modulePackage = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.METADATA,
                        Map.of("module", "crm.contract"))),
                new LowCodePackageDependencyManifest(List.of(
                        new LowCodePackageDependency(LowCodePackageDependencyType.ACTION,
                                null, "crm.contract", null, true)
                )),
                null
        );

        assertThatThrownBy(() -> validator.validate(modulePackage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACTION dependency actionCode must not be blank");
    }
}
