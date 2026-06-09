package net.ximatai.muyun.spring.platform.exchange.model;

import net.ximatai.muyun.spring.common.exception.PlatformException;

import java.util.List;

public record ExcelSheetPlan(
        String sheetName,
        String entityAlias,
        boolean main,
        List<ExcelColumnPlan> columns,
        List<List<Object>> rows
) {
    public ExcelSheetPlan {
        requireText(sheetName, "sheetName must not be blank");
        requireText(entityAlias, "entityAlias must not be blank");
        if (columns == null || columns.isEmpty()) {
            throw new PlatformException("sheet columns must not be empty");
        }
        columns = List.copyOf(columns);
        rows = copyRows(rows);
    }

    public ExcelSheetPlan(String sheetName, String entityAlias, boolean main, List<ExcelColumnPlan> columns) {
        this(sheetName, entityAlias, main, columns, List.of());
    }

    private static List<List<Object>> copyRows(List<List<Object>> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .map(row -> row == null ? List.<Object>of() : List.copyOf(row))
                .toList();
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(message);
        }
    }
}
