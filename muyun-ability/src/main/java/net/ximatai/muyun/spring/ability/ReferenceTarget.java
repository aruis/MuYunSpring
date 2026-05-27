package net.ximatai.muyun.spring.ability;

import java.util.Objects;

public record ReferenceTarget(String moduleAlias, String entityCode) {
    private static final String SEPARATOR = ".";

    public ReferenceTarget {
        moduleAlias = requireText(moduleAlias, "moduleAlias");
        entityCode = requireText(entityCode, "entityCode");
    }

    public static ReferenceTarget of(String moduleAlias, String entityCode) {
        return new ReferenceTarget(moduleAlias, entityCode);
    }

    public static ReferenceTarget parse(String value) {
        String normalized = requireText(value, "value");
        int separatorIndex = normalized.lastIndexOf(SEPARATOR);
        if (separatorIndex <= 0 || separatorIndex == normalized.length() - 1) {
            throw new IllegalArgumentException("reference target must use '<moduleAlias>.<entityCode>': " + value);
        }
        return new ReferenceTarget(
                normalized.substring(0, separatorIndex),
                normalized.substring(separatorIndex + 1)
        );
    }

    public String qualifiedName() {
        return moduleAlias + SEPARATOR + entityCode;
    }

    private static String requireText(String value, String name) {
        String text = Objects.requireNonNull(value, name + " must not be null").trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }
}
