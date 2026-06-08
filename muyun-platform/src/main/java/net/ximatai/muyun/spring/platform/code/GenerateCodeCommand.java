package net.ximatai.muyun.spring.platform.code;

import java.time.LocalDateTime;
import java.util.Map;

public record GenerateCodeCommand(
        String moduleAlias,
        String entityAlias,
        String metadataFieldId,
        String fieldName,
        String organizationId,
        LocalDateTime at,
        Map<String, Object> context,
        CodeValueUniquenessChecker uniquenessChecker
) {
}
