package net.ximatai.muyun.spring.common.model.constraint;

import java.util.LinkedHashSet;
import java.util.List;

public record TenantUniqueConstraintDefinition(List<String> fieldNames, String message) {
    public TenantUniqueConstraintDefinition {
        fieldNames = fieldNames == null ? List.of() : fieldNames.stream()
                .filter(fieldName -> fieldName != null && !fieldName.isBlank())
                .map(String::trim)
                .toList();
        if (fieldNames.isEmpty()) {
            throw new IllegalArgumentException("tenant unique constraint requires fields");
        }
        if (new LinkedHashSet<>(fieldNames).size() != fieldNames.size()) {
            throw new IllegalArgumentException("tenant unique constraint fields must be distinct");
        }
        message = message == null ? "" : message.trim();
    }

    public String violationMessage() {
        return message.isBlank()
                ? "tenant unique constraint violated: " + String.join(", ", fieldNames)
                : message;
    }
}
