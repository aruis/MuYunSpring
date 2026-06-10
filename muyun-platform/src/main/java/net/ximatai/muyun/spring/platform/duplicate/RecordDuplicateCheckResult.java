package net.ximatai.muyun.spring.platform.duplicate;

import java.util.List;

public record RecordDuplicateCheckResult(
        String ruleId,
        String actionCode,
        List<String> fieldNames,
        boolean duplicated,
        List<RecordDuplicateMatch> matches
) {
    public RecordDuplicateCheckResult {
        fieldNames = fieldNames == null ? List.of() : List.copyOf(fieldNames);
        matches = matches == null ? List.of() : List.copyOf(matches);
    }
}
