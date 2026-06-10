package net.ximatai.muyun.spring.platform.code;

public record CodeBusinessPreviewItem(
        String ruleId,
        String moduleMetadataFieldId,
        String metadataFieldId,
        String fieldName,
        CodeFieldRole fieldRole,
        String value,
        String organizationId,
        String effectiveAt
) {
    public CodeBusinessPreviewItem(String ruleId,
                                   String metadataFieldId,
                                   String fieldName,
                                   CodeFieldRole fieldRole,
                                   String value,
                                   String organizationId,
                                   String effectiveAt) {
        this(ruleId, null, metadataFieldId, fieldName, fieldRole, value, organizationId, effectiveAt);
    }
}
