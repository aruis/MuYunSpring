package net.ximatai.muyun.spring.ability.reference;

import net.ximatai.muyun.spring.common.util.Preconditions;

public record ReferenceTarget(String moduleAlias, String entityCode) {
    private static final String SEPARATOR = ".";

    public ReferenceTarget {
        moduleAlias = Preconditions.requireText(moduleAlias, "moduleAlias");
        entityCode = Preconditions.requireText(entityCode, "entityCode");
    }

    public static ReferenceTarget of(String moduleAlias, String entityCode) {
        return new ReferenceTarget(moduleAlias, entityCode);
    }

    public static ReferenceTarget parse(String value) {
        String normalized = Preconditions.requireText(value, "value");
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

}
