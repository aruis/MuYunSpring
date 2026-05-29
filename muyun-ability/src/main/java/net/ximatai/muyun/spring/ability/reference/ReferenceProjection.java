package net.ximatai.muyun.spring.ability.reference;

import net.ximatai.muyun.spring.common.exception.PlatformException;
public record ReferenceProjection(String targetField, String outputField) {
    public ReferenceProjection {
        if (targetField == null || targetField.isBlank()) {
            throw new PlatformException("reference projection targetField must not be blank");
        }
        if (outputField == null || outputField.isBlank()) {
            throw new PlatformException("reference projection outputField must not be blank");
        }
    }

    public static ReferenceProjection of(String targetField, String outputField) {
        return new ReferenceProjection(targetField, outputField);
    }
}
