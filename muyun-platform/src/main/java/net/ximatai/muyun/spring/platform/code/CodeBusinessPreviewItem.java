package net.ximatai.muyun.spring.platform.code;

public record CodeBusinessPreviewItem(
        String ruleId,
        String metadataFieldId,
        String fieldName,
        CodeFieldRole fieldRole,
        String value,
        String organizationId
) {
}
