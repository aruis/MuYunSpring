package net.ximatai.muyun.spring.platform.config;

public record LowCodeModuleConfigPublishResult(
        LowCodeModuleConfigVersion version,
        LowCodeConfigHealthReport healthReport
) {
}
