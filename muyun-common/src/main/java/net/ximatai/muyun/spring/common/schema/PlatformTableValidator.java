package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.builder.Column;
import net.ximatai.muyun.database.core.builder.TableWrapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class PlatformTableValidator {
    public void requireStandardEntityTable(TableWrapper table, String source) {
        Objects.requireNonNull(table, "table must not be null");
        Map<String, Column> columns = columnMap(table);
        requireColumn(columns, StandardEntitySchema.idColumn(), source, true);
        for (Column expected : StandardEntitySchema.auditColumns()) {
            requireColumn(columns, expected, source, false);
        }
    }

    private void requireColumn(Map<String, Column> columns, Column expected, String source, boolean primaryKey) {
        Column actual = columns.get(expected.getName());
        if (actual == null) {
            throw new IllegalArgumentException(
                    "platform table missing standard column %s: %s".formatted(expected.getName(), source)
            );
        }
        if (primaryKey && !actual.isPrimaryKey()) {
            throw new IllegalArgumentException(
                    "platform table standard column must be primary key %s: %s".formatted(expected.getName(), source)
            );
        }
        if (actual.getType() != expected.getType()) {
            throw new IllegalArgumentException(
                    "platform table standard column type mismatch %s: %s".formatted(expected.getName(), source)
            );
        }
        if (!Objects.equals(actual.getLength(), expected.getLength())
                || !Objects.equals(actual.getPrecision(), expected.getPrecision())
                || !Objects.equals(actual.getScale(), expected.getScale())) {
            throw new IllegalArgumentException(
                    "platform table standard column size mismatch %s: %s".formatted(expected.getName(), source)
            );
        }
        if (actual.isNullable() != expected.isNullable()) {
            throw new IllegalArgumentException(
                    "platform table standard column nullable mismatch %s: %s".formatted(expected.getName(), source)
            );
        }
    }

    private Map<String, Column> columnMap(TableWrapper table) {
        Map<String, Column> columns = new LinkedHashMap<>();
        if (table.getPrimaryKey() != null) {
            columns.put(table.getPrimaryKey().getName(), table.getPrimaryKey());
        }
        table.getColumns().forEach(column -> columns.put(column.getName(), column));
        return columns;
    }
}
