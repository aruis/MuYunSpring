package net.ximatai.muyun.spring.ability.reference;

import net.ximatai.muyun.spring.common.util.Preconditions;

public record ReferenceTarget(String moduleAlias, String entityAlias) {
    private static final String SEPARATOR = ".";

    public ReferenceTarget {
        moduleAlias = Preconditions.requireText(moduleAlias, "moduleAlias");
        entityAlias = Preconditions.requireText(entityAlias, "entityAlias");
    }

    public static ReferenceTarget of(String moduleAlias, String entityAlias) {
        return new ReferenceTarget(moduleAlias, entityAlias);
    }

    public static ReferenceTarget parse(String value) {
        String normalized = Preconditions.requireText(value, "value");
        int separatorIndex = normalized.lastIndexOf(SEPARATOR);
        if (separatorIndex <= 0 || separatorIndex == normalized.length() - 1) {
            throw new IllegalArgumentException("reference target must use '<moduleAlias>.<entityAlias>': " + value);
        }
        return new ReferenceTarget(
                normalized.substring(0, separatorIndex),
                normalized.substring(separatorIndex + 1)
        );
    }

    public String qualifiedName() {
        return moduleAlias + SEPARATOR + entityAlias;
    }

}
