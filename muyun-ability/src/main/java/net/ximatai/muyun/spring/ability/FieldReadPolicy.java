package net.ximatai.muyun.spring.ability;

import java.util.Collection;
import java.util.Set;

public record FieldReadPolicy(boolean allFieldsReadable,
                              Set<String> readableFields) {
    public FieldReadPolicy {
        readableFields = readableFields == null
                ? Set.of()
                : readableFields.stream()
                .filter(fieldName -> fieldName != null && !fieldName.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public static FieldReadPolicy allReadable() {
        return new FieldReadPolicy(true, Set.of());
    }

    public static FieldReadPolicy readableFields(Collection<String> fieldNames) {
        return new FieldReadPolicy(false, fieldNames == null
                ? Set.of()
                : fieldNames.stream()
                .filter(fieldName -> fieldName != null && !fieldName.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }

    public boolean allows(String fieldName) {
        if (allFieldsReadable) {
            return true;
        }
        return fieldName != null && readableFields.contains(fieldName);
    }

    public boolean restricted() {
        return !allFieldsReadable;
    }
}
