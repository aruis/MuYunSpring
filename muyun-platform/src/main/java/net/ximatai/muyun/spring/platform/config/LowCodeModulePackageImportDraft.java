package net.ximatai.muyun.spring.platform.config;

import java.time.Instant;

public record LowCodeModulePackageImportDraft(
        String draftId,
        LowCodeModulePackage modulePackage,
        LowCodePackageDryRunResult dryRunResult,
        String baseVersionId,
        Instant createdAt
) {
    public LowCodeModulePackageImportDraft {
        if (draftId == null || draftId.isBlank()) {
            throw new IllegalArgumentException("draftId must not be blank");
        }
        if (modulePackage == null) {
            throw new IllegalArgumentException("modulePackage must not be null");
        }
        if (dryRunResult == null) {
            throw new IllegalArgumentException("dryRunResult must not be null");
        }
        baseVersionId = normalize(baseVersionId);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public String moduleAlias() {
        return modulePackage.moduleAlias();
    }

    public LowCodePackageMode mode() {
        return modulePackage.mode();
    }

    public boolean publishable() {
        return !dryRunResult.blocked();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
