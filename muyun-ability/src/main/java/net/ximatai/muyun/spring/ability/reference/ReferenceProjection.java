package net.ximatai.muyun.spring.ability.reference;

import net.ximatai.muyun.spring.ability.AbilityException;

public record ReferenceProjection(String targetField, String outputField) {
    public ReferenceProjection {
        if (targetField == null || targetField.isBlank()) {
            throw new AbilityException("reference projection targetField must not be blank");
        }
        if (outputField == null || outputField.isBlank()) {
            throw new AbilityException("reference projection outputField must not be blank");
        }
    }

    public static ReferenceProjection of(String targetField, String outputField) {
        return new ReferenceProjection(targetField, outputField);
    }
}
