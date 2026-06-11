package net.ximatai.muyun.spring.platform.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Service
public class LowCodeModuleConfigPublishFacade {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final LowCodeModuleConfigVersionService versionService;
    private final LowCodeModuleHealthService healthService;

    public LowCodeModuleConfigPublishFacade(LowCodeModuleConfigVersionService versionService,
                                            LowCodeModuleHealthService healthService) {
        this.versionService = versionService;
        this.healthService = healthService;
    }

    @Transactional
    public LowCodeModuleConfigPublishResult publish(LowCodeModulePackage modulePackage,
                                                    String operatorId,
                                                    String remark) {
        LowCodeConfigHealthReport healthReport = healthService.check(LowCodeModuleHealthContext.ofPackage(modulePackage));
        if (healthReport.status() == LowCodeConfigHealthStatus.FAIL) {
            throw new PlatformException("low code module config health check failed: " + modulePackage.moduleAlias());
        }
        String snapshot = snapshot(modulePackage);
        LowCodeModuleConfigVersion version = new LowCodeModuleConfigVersion();
        version.setModuleAlias(modulePackage.moduleAlias());
        version.setVersionNo(versionService.nextVersionNo(modulePackage.moduleAlias()));
        version.setVersionStatus(LowCodeConfigVersionStatus.PUBLISHED);
        version.setCurrentVersion(Boolean.FALSE);
        version.setPackageSnapshotText(snapshot);
        version.setPackageHash(sha256(snapshot));
        version.setSummaryJson(summary(modulePackage, healthReport));
        version.setSourceVersionId(modulePackage.publishManifest().sourceVersionId());
        version.setPublishedBy(normalize(operatorId));
        version.setPublishedAt(Instant.now());
        version.setRemark(normalize(remark));
        versionService.insert(version);
        versionService.markOnlyCurrent(modulePackage.moduleAlias(), version.getId());
        return new LowCodeModuleConfigPublishResult(version, healthReport);
    }

    @Transactional
    public LowCodeModuleConfigVersion rollback(String moduleAlias, String versionId) {
        String validModuleAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        versionService.markOnlyCurrent(validModuleAlias, versionId);
        return versionService.select(versionId);
    }

    private String snapshot(LowCodeModulePackage modulePackage) {
        try {
            return OBJECT_MAPPER.writeValueAsString(modulePackage);
        } catch (JsonProcessingException exception) {
            throw new PlatformException("low code module package snapshot cannot be encoded: "
                    + modulePackage.moduleAlias());
        }
    }

    private String summary(LowCodeModulePackage modulePackage, LowCodeConfigHealthReport healthReport) {
        try {
            return OBJECT_MAPPER.writeValueAsString(new Summary(
                    modulePackage.mode(),
                    modulePackage.bundles().stream().filter(LowCodeConfigBundle::included).map(LowCodeConfigBundle::type).toList(),
                    healthReport.status(),
                    healthReport.items().size()
            ));
        } catch (JsonProcessingException exception) {
            throw new PlatformException("low code module config summary cannot be encoded: "
                    + modulePackage.moduleAlias());
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record Summary(
            LowCodePackageMode mode,
            java.util.List<LowCodePackageBundleType> includedBundles,
            LowCodeConfigHealthStatus healthStatus,
            int healthItemCount
    ) {
    }
}
