package net.ximatai.muyun.spring.ability.action;

public record DataChangeIntent(
        DataChangeOperation operation,
        Class<?> moduleType,
        String recordId
) {
    public DataChangeIntent {
        if (operation == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
        if (moduleType == null) {
            throw new IllegalArgumentException("moduleType must not be null");
        }
        if (recordId == null || recordId.isBlank()) {
            throw new IllegalArgumentException("recordId must not be blank");
        }
        recordId = recordId.trim();
    }
}
