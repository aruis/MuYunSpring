package net.ximatai.muyun.spring.ability.query;

public enum QueryOperator {
    EQ,
    NOT_EQUAL,
    LIKE,
    IN,
    NOT_IN,
    GT,
    GTE,
    LT,
    LTE,
    BETWEEN,
    NULL,
    NOT_NULL;

    public static QueryOperator from(String value) {
        if (value == null || value.isBlank()) {
            return EQ;
        }
        return switch (value.trim().toUpperCase()) {
            case "=", "EQ" -> EQ;
            case "!=", "<>", "NE", "NOT_EQUAL" -> NOT_EQUAL;
            case "LIKE" -> LIKE;
            case "IN" -> IN;
            case "NOT_IN", "NOT IN" -> NOT_IN;
            case ">", "GT" -> GT;
            case ">=", "GTE" -> GTE;
            case "<", "LT" -> LT;
            case "<=", "LTE" -> LTE;
            case "BETWEEN" -> BETWEEN;
            case "NULL", "IS_NULL", "IS NULL" -> NULL;
            case "NOT_NULL", "IS_NOT_NULL", "IS NOT NULL" -> NOT_NULL;
            default -> throw new IllegalArgumentException("query operator is not supported: " + value);
        };
    }
}
