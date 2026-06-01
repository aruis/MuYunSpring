package net.ximatai.muyun.spring.dynamic.metadata;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class FieldBehaviorSupport {
    private FieldBehaviorSupport() {
    }

    public static Object parseDefaultValue(FieldType type, String value) {
        return switch (type) {
            case STRING, TEXT, JSON -> value;
            case INTEGER -> Integer.valueOf(value);
            case LONG -> Long.valueOf(value);
            case BOOLEAN -> parseBoolean(value);
            case DECIMAL -> new BigDecimal(value);
            case TIMESTAMP -> Instant.parse(value);
            case DATE -> LocalDate.parse(value);
        };
    }

    public static void validateBehavior(FieldType type, FieldBehaviorDefinition behavior, String fieldCode) {
        if (behavior.validationRegex() != null) {
            if (type != FieldType.STRING && type != FieldType.TEXT) {
                throw new IllegalArgumentException("validationRegex requires string field: " + fieldCode);
            }
            java.util.regex.Pattern.compile(behavior.validationRegex());
        }
        if (behavior.defaultValue() != null) {
            Object parsed = parseDefaultValue(type, behavior.defaultValue());
            if (behavior.validationRegex() != null
                    && parsed instanceof String text
                    && !text.matches(behavior.validationRegex())) {
                throw new IllegalArgumentException("defaultValue does not match validationRegex: " + fieldCode);
            }
        }
    }

    private static Boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("boolean defaultValue must be true or false");
    }
}
