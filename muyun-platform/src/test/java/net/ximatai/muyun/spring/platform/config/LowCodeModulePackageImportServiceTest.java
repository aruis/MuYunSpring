package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static net.ximatai.muyun.spring.platform.config.LowCodeConfigTestFixtures.fullPackage;
import static net.ximatai.muyun.spring.platform.config.LowCodeConfigTestFixtures.pageOnlyPackage;
import static net.ximatai.muyun.spring.platform.config.LowCodeConfigTestFixtures.templatePackage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LowCodeModulePackageImportServiceTest {
    private final LowCodeModuleConfigVersionService versionService =
            new LowCodeModuleConfigVersionService(new TestMemoryDao<>());
    private final LowCodeModuleHealthService healthService =
            new LowCodeModuleHealthService(List.of(new LowCodeModulePackageHealthChecker()));
    private final LowCodeModuleConfigArchiveFacade archiveFacade =
            new LowCodeModuleConfigArchiveFacade(versionService, healthService);
    private final LowCodeModulePackageExchangeService exchangeService =
            new LowCodeModulePackageExchangeService(versionService, healthService);
    private final LowCodeModulePackageImportService importService =
            new LowCodeModulePackageImportService(exchangeService, versionService, archiveFacade);

    @Test
    void shouldPrepareDraftForNewFullModulePackage() {
        LowCodeModulePackageImportDraft draft = importService.prepareDraft(fullPackage("crm.contract"));

        assertThat(draft.dryRunPassed()).isTrue();
        assertThat(draft.moduleAlias()).isEqualTo("crm.contract");
        assertThat(draft.mode()).isEqualTo(LowCodePackageMode.MODULE_FULL);
        assertThat(draft.baseVersionId()).isNull();
        assertThat(draft.dryRunResult().status()).isEqualTo(LowCodePackageDryRunStatus.READY);
    }

    @Test
    void shouldPrepareDraftOverExistingModuleWithBaseVersion() {
        LowCodeModuleConfigVersion current = archiveFacade.archive(fullPackage("crm.contract"), "tester", "baseline").version();

        LowCodeModulePackageImportDraft draft = importService.prepareDraft(fullPackage("crm.contract"));

        assertThat(draft.dryRunPassed()).isTrue();
        assertThat(draft.baseVersionId()).isEqualTo(current.getId());
        assertThat(draft.dryRunResult().status()).isEqualTo(LowCodePackageDryRunStatus.WARN);
    }

    @Test
    void shouldRejectBlockedDryRunWhenPreparingDraft() {
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

        assertThatThrownBy(() -> importService.prepareDraft(invalid))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("low code package import dry-run is blocked: crm.contract");
    }

    @Test
    void shouldArchiveImportDraftThroughExistingArchiveFacade() {
        LowCodeModulePackageImportDraft draft = importService.prepareDraft(fullPackage("crm.contract"));

        LowCodeModuleConfigArchiveResult result = importService.archiveDraft(draft, "importer", "import draft");

        assertThat(result.version().getVersionNo()).isEqualTo(1);
        assertThat(result.version().getCurrentVersion()).isTrue();
        assertThat(result.version().getArchivedBy()).isEqualTo("importer");
        assertThat(versionService.currentVersion("crm.contract").getId()).isEqualTo(result.version().getId());
    }

    @Test
    void shouldRejectArchiveWhenDraftBaseVersionIsStale() {
        LowCodeModuleConfigVersion baseline = archiveFacade.archive(fullPackage("crm.contract"), "tester", "baseline").version();
        LowCodeModulePackageImportDraft draft = importService.prepareDraft(fullPackage("crm.contract"));
        assertThat(draft.baseVersionId()).isEqualTo(baseline.getId());
        archiveFacade.archive(fullPackage("crm.contract"), "tester", "new current");

        assertThatThrownBy(() -> importService.archiveDraft(draft, "importer", "stale"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("low code package import draft base version is stale: crm.contract");
    }

    @Test
    void shouldAllowPageOnlyDraftOnlyWhenTargetHasCurrentVersion() {
        assertThatThrownBy(() -> importService.prepareDraft(pageOnlyPackage("crm.contract")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("low code package import dry-run is blocked: crm.contract");

        LowCodeModuleConfigVersion current = archiveFacade.archive(fullPackage("crm.contract"), "tester", "baseline").version();

        LowCodeModulePackageImportDraft draft = importService.prepareDraft(pageOnlyPackage("crm.contract"));

        assertThat(draft.dryRunPassed()).isTrue();
        assertThat(draft.baseVersionId()).isEqualTo(current.getId());
        assertThat(draft.mode()).isEqualTo(LowCodePackageMode.PAGE_ONLY);
    }

    @Test
    void shouldRejectArchivingPageOnlyDraftWithoutMergeSupport() {
        archiveFacade.archive(fullPackage("crm.contract"), "tester", "baseline");
        LowCodeModulePackageImportDraft draft = importService.prepareDraft(pageOnlyPackage("crm.contract"));

        assertThatThrownBy(() -> importService.archiveDraft(draft, "importer", "page only"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("only MODULE_FULL import draft can be archived: crm.contract");
    }

    @Test
    void shouldRejectArchivingTemplateDraftWithoutInstantiation() {
        LowCodeModulePackageImportDraft draft = importService.prepareDraft(templatePackage("crm.contract"));

        assertThatThrownBy(() -> importService.archiveDraft(draft, "importer", "template"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("only MODULE_FULL import draft can be archived: crm.contract");
    }
}
