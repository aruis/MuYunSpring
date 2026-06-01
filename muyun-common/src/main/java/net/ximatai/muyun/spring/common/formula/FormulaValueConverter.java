package net.ximatai.muyun.spring.common.formula;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

final class FormulaValueConverter {
    private FormulaValueConverter() {
    }

    static Object convertForWrite(FormulaFieldDefinition field, Object value) {
        if (field == null || field.type() == FormulaValueType.ANY) {
            return value;
        }
        if (value == null) {
            if (field.required()) {
                throw new FormulaEvaluationException(
                        "FORMULA_REQUIRED_FIELD_EMPTY",
                        field.fieldPath().dataIndex(),
                        "formula field is required: " + field.fieldPath().dataIndex()
                );
            }
            return null;
        }
        try {
            return switch (field.type()) {
                case ANY, JSON -> value;
                case STRING, TEXT -> String.valueOf(value);
                case INTEGER -> toInteger(field, value);
                case LONG -> toLong(field, value);
                case DECIMAL -> toDecimal(field, value);
                case BOOLEAN -> toBoolean(field, value);
                case DATE -> toDate(field, value);
                case TIMESTAMP -> toTimestamp(field, value);
            };
        } catch (FormulaEvaluationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw typeMismatch(field, value);
        }
    }

    private static Integer toInteger(FormulaFieldDefinition field, Object value) {
        long number = toIntegral(field, value);
        if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
            throw typeMismatch(field, value);
        }
        return (int) number;
    }

    private static Long toLong(FormulaFieldDefinition field, Object value) {
        return toIntegral(field, value);
    }

    private static long toIntegral(FormulaFieldDefinition field, Object value) {
        if (value instanceof Number number) {
            return integralNumber(field, value, number);
        }
        String text = String.valueOf(value).trim();
        if (!text.matches("[-+]?\\d+")) {
            throw typeMismatch(field, value);
        }
        return Long.parseLong(text);
    }

    private static long integralNumber(FormulaFieldDefinition field, Object value, Number number) {
        if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long) {
            return number.longValue();
        }
        if (number instanceof BigInteger bigInteger) {
            return bigInteger.longValueExact();
        }
        if (number instanceof BigDecimal decimal) {
            return decimal.toBigIntegerExact().longValueExact();
        }
        double numeric = number.doubleValue();
        if (Double.isFinite(numeric) && numeric == Math.rint(numeric) && Math.abs(numeric) <= 9_007_199_254_740_991d) {
            return (long) numeric;
        }
        throw typeMismatch(field, value);
    }

    private static BigDecimal toDecimal(FormulaFieldDefinition field, Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number || value instanceof CharSequence) {
            return new BigDecimal(String.valueOf(value).trim());
        }
        throw typeMismatch(field, value);
    }

    private static Boolean toBoolean(FormulaFieldDefinition field, Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.doubleValue() != 0d;
        }
        String text = String.valueOf(value).trim();
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        throw typeMismatch(field, value);
    }

    private static LocalDate toDate(FormulaFieldDefinition field, Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof Instant instant) {
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC).toLocalDate();
        }
        try {
            return LocalDate.parse(String.valueOf(value).trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            throw typeMismatch(field, value);
        }
    }

    private static Instant toTimestamp(FormulaFieldDefinition field, Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toInstant(ZoneOffset.UTC);
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        String text = String.valueOf(value).trim();
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ex) {
            throw typeMismatch(field, value);
        }
    }

    private static FormulaEvaluationException typeMismatch(FormulaFieldDefinition field, Object value) {
        return new FormulaEvaluationException(
                "FORMULA_TYPE_MISMATCH",
                field.fieldPath().dataIndex(),
                "formula field [%s] requires %s but got [%s]"
                        .formatted(field.fieldPath().dataIndex(), field.type(), value)
        );
    }
}
