package net.ximatai.muyun.spring.platform.code;

import java.time.LocalDateTime;
import java.util.Map;

public record PreviewCodeRuleCommand(
        CodeRule rule,
        Map<String, Object> context,
        String organizationId,
        LocalDateTime at,
        Long sequenceValue
) {
}
