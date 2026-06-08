package net.ximatai.muyun.spring.platform.code;

import java.time.LocalDateTime;

public record ResolveCodeRuleCommand(
        String moduleAlias,
        String entityAlias,
        String metadataFieldId,
        String fieldName,
        String organizationId,
        LocalDateTime at
) {
}
