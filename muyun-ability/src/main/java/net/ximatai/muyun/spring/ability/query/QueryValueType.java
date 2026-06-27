package net.ximatai.muyun.spring.ability.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

public enum QueryValueType {
    STRING,
    TEXT,
    BOOLEAN,
    INTEGER,
    LONG,
    DECIMAL,
    INSTANT,
    DATE,
    JSON;

    private static final Pattern UTC_INSTANT_SECONDS = Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"
    );

    public QueryOperator defaultOperator() {
        return switch (this) {
            case STRING, TEXT -> QueryOperator.LIKE;
            default -> QueryOperator.EQ;
        };
    }

    public Object normalize(Object value) {
        if (value == null) {
            return null;
        }
        return switch (this) {
            case STRING, TEXT -> requireType(value, String.class);
            case BOOLEAN -> booleanValue(value);
            case INTEGER -> integerValue(value);
            case LONG -> longValue(value);
            case DECIMAL -> decimalValue(value);
            case INSTANT -> instantValue(value);
            case DATE -> dateValue(value);
            case JSON -> value;
        };
    }

    private Object requireType(Object value, Class<?> type) {
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("invalid query value type: " + this);
        }
        return value;
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String text) {
            if ("true".equalsIgnoreCase(text.trim())) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(text.trim())) {
                return Boolean.FALSE;
            }
        }
        throw new IllegalArgumentException("invalid query value type: " + this);
    }

    private Integer integerValue(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            return Integer.valueOf(text.trim());
        }
        throw new IllegalArgumentException("invalid query value type: " + this);
    }

    private Long longValue(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            return Long.valueOf(text.trim());
        }
        throw new IllegalArgumentException("invalid query value type: " + this);
    }

    private Object decimalValue(Object value) {
        if (value instanceof BigDecimal) {
            return value;
        }
        if (value instanceof Number) {
            return value;
        }
        if (value instanceof String text) {
            return new BigDecimal(text.trim());
        }
        throw new IllegalArgumentException("invalid query value type: " + this);
    }

    private Instant instantValue(Object value) {
        if (value instanceof Instant instant) {
            return requireSecondPrecision(instant);
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            if (!UTC_INSTANT_SECONDS.matcher(trimmed).matches()) {
                throw new IllegalArgumentException("timestamp must be a UTC second instant");
            }
            return requireSecondPrecision(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(trimmed)));
        }
        throw new IllegalArgumentException("invalid query value type: " + this);
    }

    private LocalDate dateValue(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof String text) {
            return LocalDate.parse(text.trim());
        }
        throw new IllegalArgumentException("invalid query value type: " + this);
    }

    private Instant requireSecondPrecision(Instant value) {
        Instant truncated = value.truncatedTo(ChronoUnit.SECONDS);
        if (!value.equals(truncated)) {
            throw new IllegalArgumentException("timestamp must use second precision");
        }
        return value;
    }
}
