package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class LowCodeModulePackageImportService {
    private final LowCodeModulePackageExchangeService exchangeService;
    private final LowCodeModuleConfigVersionService versionService;
    private final LowCodeModuleConfigArchiveFacade archiveFacade;

    public LowCodeModulePackageImportService(LowCodeModulePackageExchangeService exchangeService,
                                             LowCodeModuleConfigVersionService versionService,
                                             LowCodeModuleConfigArchiveFacade archiveFacade) {
        this.exchangeService = exchangeService;
        this.versionService = versionService;
        this.archiveFacade = archiveFacade;
    }

    public LowCodeModulePackageImportDraft prepareDraft(String packageJson) {
        return prepareDraft(exchangeService.parsePackage(packageJson));
    }

    public LowCodeModulePackageImportDraft prepareDraft(LowCodeModulePackage modulePackage) {
        LowCodePackageDryRunResult dryRunResult = exchangeService.dryRunImport(modulePackage);
        if (dryRunResult.blocked()) {
            throw new PlatformException("low code package import dry-run is blocked: " + modulePackage.moduleAlias());
        }
        LowCodeModuleConfigVersion current = versionService.currentVersion(modulePackage.moduleAlias());
        return new LowCodeModulePackageImportDraft(
                UUID.randomUUID().toString(),
                modulePackage,
                dryRunResult,
                current == null ? null : current.getId(),
                Instant.now()
        );
    }

    public LowCodeModuleConfigArchiveResult archiveDraft(LowCodeModulePackageImportDraft draft,
                                                         String operatorId,
                                                         String remark) {
        if (draft == null) {
            throw new IllegalArgumentException("import draft must not be null");
        }
        if (!draft.dryRunPassed()) {
            throw new PlatformException("low code package import draft is not ready to archive: " + draft.moduleAlias());
        }
        if (draft.mode() != LowCodePackageMode.MODULE_FULL) {
            throw new PlatformException("only MODULE_FULL import draft can be archived: " + draft.moduleAlias());
        }
        rejectStaleDraft(draft);
        return archiveFacade.archive(draft.modulePackage(), operatorId, remark);
    }

    private void rejectStaleDraft(LowCodeModulePackageImportDraft draft) {
        LowCodeModuleConfigVersion current = versionService.currentVersion(draft.moduleAlias());
        String currentVersionId = current == null ? null : current.getId();
        if (!java.util.Objects.equals(currentVersionId, draft.baseVersionId())) {
            throw new PlatformException("low code package import draft base version is stale: " + draft.moduleAlias());
        }
    }
}
