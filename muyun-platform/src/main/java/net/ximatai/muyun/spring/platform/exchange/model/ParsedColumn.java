package net.ximatai.muyun.spring.platform.exchange.model;

import net.ximatai.muyun.spring.common.exception.PlatformException;

public record ParsedColumn(
        int columnIndex,
        String entityAlias,
        String fieldName,
        String title
) {
    public ParsedColumn {
        if (columnIndex < 0) {
            throw new PlatformException("columnIndex must not be negative");
        }
        requireText(fieldName, "fieldName must not be blank");
        requireText(title, "title must not be blank");
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(message);
        }
    }
}
