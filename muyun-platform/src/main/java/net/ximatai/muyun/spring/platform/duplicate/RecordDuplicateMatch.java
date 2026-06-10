package net.ximatai.muyun.spring.platform.duplicate;

import java.util.Map;

public record RecordDuplicateMatch(
        String recordId,
        Integer version,
        Map<String, Object> values
) {
    public RecordDuplicateMatch {
        values = values == null ? Map.of() : Map.copyOf(values);
    }
}
