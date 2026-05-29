package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.spring.dynamic.metadata.FieldType;

final class FieldShapeRules {
    private FieldShapeRules() {
    }

    static void validate(FieldType type, Integer length, Integer precision, Integer scale, String context) {
        if (length != null && length <= 0) {
            throw new IllegalArgumentException("field length must be positive: " + context);
        }
        if (length != null && type != FieldType.STRING && type != FieldType.TEXT) {
            throw new IllegalArgumentException("field length only applies to string fields: " + context);
        }
        if (precision != null && precision <= 0) {
            throw new IllegalArgumentException("field precision must be positive: " + context);
        }
        if (scale != null && scale < 0) {
            throw new IllegalArgumentException("field scale must not be negative: " + context);
        }
        if ((precision != null || scale != null) && type != FieldType.DECIMAL) {
            throw new IllegalArgumentException("field precision and scale only apply to decimal fields: " + context);
        }
        if (scale != null && precision == null) {
            throw new IllegalArgumentException("field scale requires precision: " + context);
        }
        if (scale != null && scale > precision) {
            throw new IllegalArgumentException("field scale must not exceed precision: " + context);
        }
    }
}
