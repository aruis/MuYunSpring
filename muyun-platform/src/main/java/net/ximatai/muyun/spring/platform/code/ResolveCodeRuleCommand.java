package net.ximatai.muyun.spring.platform.code;

import java.time.LocalDateTime;

public record ResolveCodeRuleCommand(
        String moduleAlias,
        String entityAlias,
        String moduleMetadataFieldId,
        String metadataFieldId,
        String fieldName,
        String organizationId,
        LocalDateTime at
) {
    public ResolveCodeRuleCommand(String moduleAlias,
                                  String entityAlias,
                                  String metadataFieldId,
                                  String fieldName,
                                  String organizationId,
                                  LocalDateTime at) {
        this(moduleAlias, entityAlias, null, metadataFieldId, fieldName, organizationId, at);
    }
}
