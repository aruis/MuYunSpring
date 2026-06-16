package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static net.ximatai.muyun.spring.platform.config.LowCodeConfigTestFixtures.fullPackage;
import static net.ximatai.muyun.spring.platform.config.LowCodeConfigTestFixtures.pageOnlyPackage;

class LowCodeModuleConfigArchiveFacadeTest {
    private final LowCodeModuleConfigVersionService versionService =
            new LowCodeModuleConfigVersionService(new TestMemoryDao<>());
    private final LowCodeModuleHealthService healthService =
            new LowCodeModuleHealthService(List.of(new LowCodeModulePackageHealthChecker()));
    private final LowCodeModuleConfigArchiveFacade facade =
            new LowCodeModuleConfigArchiveFacade(versionService, healthService);

    @Test
    void shouldArchiveModulePackageAsCurrentVersion() {
        LowCodeModuleConfigArchiveResult result = facade.archive(fullPackage("crm.contract"), "tester", "baseline");

        LowCodeModuleConfigVersion version = result.version();
        assertThat(version.getVersionNo()).isEqualTo(1);
        assertThat(version.getCurrentVersion()).isTrue();
        assertThat(version.getVersionStatus()).isEqualTo(LowCodeConfigVersionStatus.ARCHIVED);
        assertThat(version.getPackageSnapshotText()).contains("\"moduleAlias\":\"crm.contract\"");
        assertThat(version.getPackageHash()).hasSize(64);
        assertThat(version.getArchivedBy()).isEqualTo("tester");
        assertThat(result.healthReport().status()).isEqualTo(LowCodeConfigHealthStatus.PASS);
        assertThat(versionService.currentVersion("crm.contract").getId()).isEqualTo(version.getId());
    }

    @Test
    void shouldArchiveNextVersionAndSwitchToHistoricalArchivedVersion() {
        LowCodeModuleConfigVersion first = facade.archive(fullPackage("crm.contract"), "tester", "v1").version();
        LowCodeModuleConfigVersion second = facade.archive(fullPackage("crm.contract"), "tester", "v2").version();

        assertThat(second.getVersionNo()).isEqualTo(2);
        assertThat(versionService.select(first.getId()).getCurrentVersion()).isFalse();
        assertThat(versionService.select(first.getId()).getVersionStatus()).isEqualTo(LowCodeConfigVersionStatus.ARCHIVED);
        assertThat(versionService.currentVersion("crm.contract").getId()).isEqualTo(second.getId());

        LowCodeModuleConfigVersion switched = facade.switchCurrentVersion("crm.contract", first.getId());

        assertThat(switched.getCurrentVersion()).isTrue();
        assertThat(versionService.select(second.getId()).getCurrentVersion()).isFalse();
        assertThat(versionService.currentVersion("crm.contract").getId()).isEqualTo(first.getId());
    }

    @Test
    void shouldRejectArchiveWhenPackageIsNotModuleFull() {
        assertThatThrownBy(() -> facade.archive(pageOnlyPackage("crm.contract"), "tester", null))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("only MODULE_FULL package can be archived: crm.contract");
        assertThat(versionService.listByModule("crm.contract")).isEmpty();
    }

    @Test
    void shouldRejectArchiveWhenHealthCheckFails() {
        LowCodeModulePackage invalid = new LowCodeModulePackage(
                "m10.v1",
                LowCodePackageMode.MODULE_FULL,
                "crm",
                "crm.contract",
                List.of(LowCodeConfigBundle.included(LowCodePackageBundleType.PAGE,
                        Map.of("moduleAlias", "crm.contract"))),
                null,
                null
        );

        assertThatThrownBy(() -> facade.archive(invalid, "tester", null))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("low code module config health check failed: crm.contract");
        assertThat(versionService.listByModule("crm.contract")).isEmpty();
    }

    @Test
    void shouldRejectSwitchingToVersionInAnotherModule() {
        LowCodeModuleConfigVersion version = facade.archive(fullPackage("crm.contract"), "tester", "v1").version();

        assertThatThrownBy(() -> facade.switchCurrentVersion("crm.customer", version.getId()))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("low code config version not found in module");
    }

    @Test
    void serviceShouldRejectDirectInsertAsCurrentVersion() {
        LowCodeModuleConfigVersion version = rawVersion("crm.contract", 1);
        version.setCurrentVersion(Boolean.TRUE);

        assertThatThrownBy(() -> versionService.insert(version))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("current version can only be switched by archive facade");
    }

    @Test
    void serviceShouldRejectArchivedSnapshotMutation() {
        LowCodeModuleConfigVersion archived = facade.archive(fullPackage("crm.contract"), "tester", "v1").version();
        LowCodeModuleConfigVersion mutated = copyVersion(archived);
        mutated.setPackageHash("changed");

        assertThatThrownBy(() -> versionService.update(mutated))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("packageHash cannot be changed");
    }

    private LowCodeModuleConfigVersion rawVersion(String moduleAlias, int versionNo) {
        LowCodeModuleConfigVersion version = new LowCodeModuleConfigVersion();
        version.setModuleAlias(moduleAlias);
        version.setVersionNo(versionNo);
        version.setVersionStatus(LowCodeConfigVersionStatus.ARCHIVED);
        version.setCurrentVersion(Boolean.FALSE);
        version.setPackageSnapshotText("{}");
        version.setPackageHash("hash");
        return version;
    }

    private LowCodeModuleConfigVersion copyVersion(LowCodeModuleConfigVersion source) {
        LowCodeModuleConfigVersion copy = new LowCodeModuleConfigVersion();
        copy.setId(source.getId());
        copy.setTenantId(source.getTenantId());
        copy.setVersion(source.getVersion());
        copy.setDeleted(source.getDeleted());
        copy.setDeletedAt(source.getDeletedAt());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        copy.setModuleAlias(source.getModuleAlias());
        copy.setVersionNo(source.getVersionNo());
        copy.setVersionStatus(source.getVersionStatus());
        copy.setCurrentVersion(source.getCurrentVersion());
        copy.setPackageSnapshotText(source.getPackageSnapshotText());
        copy.setPackageHash(source.getPackageHash());
        copy.setSummaryJson(source.getSummaryJson());
        copy.setSourceVersionId(source.getSourceVersionId());
        copy.setArchivedBy(source.getArchivedBy());
        copy.setArchivedAt(source.getArchivedAt());
        copy.setRemark(source.getRemark());
        return copy;
    }
}
