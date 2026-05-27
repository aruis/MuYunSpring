package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.builder.Column;
import net.ximatai.muyun.database.core.builder.Index;
import net.ximatai.muyun.database.core.builder.TableWrapper;

import java.util.ArrayList;
import java.util.List;

public final class PlatformUniqueIndexes {
    private PlatformUniqueIndexes() {
    }

    public static void normalizeTenantUniqueIndexes(TableWrapper table) {
        if (table == null) {
            return;
        }
        List<Index> normalized = new ArrayList<>();
        for (Index index : table.getIndexes()) {
            if (!index.isUnique() || shouldKeepGlobal(index.getColumns())) {
                normalized.add(index);
                continue;
            }
            List<String> columns = withTenant(index.getColumns());
            if (!containsIndex(normalized, columns, true)) {
                normalized.add(new Index(columns, true));
            }
        }
        table.getIndexes().clear();
        table.getIndexes().addAll(normalized);
        for (Column column : table.getColumns()) {
            if (column.isUnique() && !shouldKeepGlobal(List.of(column.getName()))) {
                column.setUnique(false);
                addTenantUniqueIndex(table, column.getName());
            }
        }
    }

    public static void addTenantUniqueIndex(TableWrapper table, String columnName) {
        if (table == null || columnName == null || columnName.isBlank() || shouldKeepGlobal(List.of(columnName))) {
            return;
        }
        List<String> columns = List.of(StandardEntitySchema.TENANT_ID_COLUMN, columnName);
        table.getIndexes().removeIf(index -> index.isUnique() && index.getColumns().equals(List.of(columnName)));
        if (!containsIndex(table.getIndexes(), columns, true)) {
            table.addIndex(columns, true);
        }
    }

    private static boolean shouldKeepGlobal(List<String> columns) {
        return columns.contains(StandardEntitySchema.ID_COLUMN)
                || columns.contains(StandardEntitySchema.TENANT_ID_COLUMN);
    }

    private static List<String> withTenant(List<String> columns) {
        List<String> result = new ArrayList<>();
        result.add(StandardEntitySchema.TENANT_ID_COLUMN);
        result.addAll(columns);
        return List.copyOf(result);
    }

    private static boolean containsIndex(List<Index> indexes, List<String> columns, boolean unique) {
        return indexes.stream().anyMatch(index -> index.isUnique() == unique && index.getColumns().equals(columns));
    }
}
