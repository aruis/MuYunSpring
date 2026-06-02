package net.ximatai.muyun.spring.dynamic.metadata;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public final class DynamicFieldValueSupport {
    private DynamicFieldValueSupport() {
    }

    public static Object normalize(FieldType type, Object value) {
        if (value == null) {
            return null;
        }
        return switch (type) {
            case STRING, TEXT -> requireType(type, value, String.class);
            case INTEGER -> requireType(type, value, Integer.class);
            case LONG -> requireType(type, value, Long.class);
            case BOOLEAN -> requireType(type, value, Boolean.class);
            case TIMESTAMP -> timestampValue(value);
            case DATE -> dateValue(value);
            case DECIMAL -> decimalValue(value);
            case JSON -> value;
        };
    }

    public static Object parseDefaultValue(FieldType type, String value) {
        if (value == null) {
            return null;
        }
        return switch (type) {
            case STRING, TEXT, JSON -> value;
            case INTEGER -> Integer.valueOf(value);
            case LONG -> Long.valueOf(value);
            case BOOLEAN -> parseBoolean(value);
            case DECIMAL -> new BigDecimal(value);
            case TIMESTAMP -> timestampValue(value);
            case DATE -> dateValue(value);
        };
    }

    private static Object requireType(FieldType type, Object value, Class<?> requiredType) {
        if (!requiredType.isInstance(value)) {
            throw new IllegalArgumentException("invalid value type for dynamic field type: " + type);
        }
        return value;
    }

    private static Instant timestampValue(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
        if (value instanceof Date date) {
            return date.toInstant();
        }
        if (value instanceof String text) {
            return Instant.parse(text);
        }
        throw new IllegalArgumentException("invalid value type for dynamic field type: " + FieldType.TIMESTAMP);
    }

    private static LocalDate dateValue(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof String text) {
            return LocalDate.parse(text);
        }
        throw new IllegalArgumentException("invalid value type for dynamic field type: " + FieldType.DATE);
    }

    private static Object decimalValue(Object value) {
        if (value instanceof BigDecimal) {
            return value;
        }
        if (value instanceof Number) {
            return value;
        }
        throw new IllegalArgumentException("invalid value type for dynamic field type: " + FieldType.DECIMAL);
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
