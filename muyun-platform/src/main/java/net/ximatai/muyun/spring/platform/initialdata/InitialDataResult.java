package net.ximatai.muyun.spring.platform.initialdata;

import java.util.List;

public record InitialDataResult(
        String key,
        InitialDataPolicy policy,
        InitialDataStatus status,
        List<String> changedFields
) {
    public InitialDataResult {
        changedFields = changedFields == null ? List.of() : List.copyOf(changedFields);
    }
}
