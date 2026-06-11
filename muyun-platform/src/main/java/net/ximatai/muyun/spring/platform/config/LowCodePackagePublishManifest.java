package net.ximatai.muyun.spring.platform.config;

import java.time.Instant;

public record LowCodePackagePublishManifest(
        String packageVersion,
        String sourceEnvironment,
        String sourceVersionId,
        String exportedBy,
        Instant exportedAt,
        String remark
) {
    public LowCodePackagePublishManifest {
        packageVersion = requireText(packageVersion, "packageVersion");
        sourceEnvironment = normalize(sourceEnvironment);
        sourceVersionId = normalize(sourceVersionId);
        exportedBy = normalize(exportedBy);
        exportedAt = exportedAt == null ? Instant.EPOCH : exportedAt;
        remark = normalize(remark);
    }

    public static LowCodePackagePublishManifest draft(String packageVersion) {
        return new LowCodePackagePublishManifest(packageVersion, null, null, null, Instant.EPOCH, null);
    }

    private static String requireText(String value, String name) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
