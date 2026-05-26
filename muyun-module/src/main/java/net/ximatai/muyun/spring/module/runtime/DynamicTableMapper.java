package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.builder.Column;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinitionValidator;

public class DynamicTableMapper {
    private final ModuleDefinitionValidator validator = new ModuleDefinitionValidator();

    public TableWrapper toTable(EntityDefinition entity) {
        validator.validateEntity(entity);
        TableWrapper table = TableWrapper.withName(entity.tableName())
                .setComment(entity.name())
                .setPrimaryKey(Column.of("id")
                        .setType(ColumnType.VARCHAR)
                        .setLength(32)
                        .setComment("ID")
                        .setPrimaryKey());
        addStandardColumns(table);
        for (FieldDefinition field : entity.fields()) {
            table.addColumn(toColumn(field));
        }
        return table;
    }

    private void addStandardColumns(TableWrapper table) {
        table.addColumn(Column.of("version").setType(ColumnType.INT).setComment("Optimistic lock version"));
        table.addColumn(Column.of("deleted").setType(ColumnType.BOOLEAN).setComment("Soft delete flag"));
        table.addColumn(Column.of("created_by").setType(ColumnType.VARCHAR).setLength(64).setComment("Created by"));
        table.addColumn(Column.of("created_at").setType(ColumnType.TIMESTAMP).setComment("Created at"));
        table.addColumn(Column.of("updated_by").setType(ColumnType.VARCHAR).setLength(64).setComment("Updated by"));
        table.addColumn(Column.of("updated_at").setType(ColumnType.TIMESTAMP).setComment("Updated at"));
    }

    private Column toColumn(FieldDefinition field) {
        Column column = Column.of(field.columnName())
                .setType(field.type().toColumnType())
                .setComment(field.name())
                .setNullable(!field.required())
                .setUnique(field.unique())
                .setIndexed(field.indexed());
        if (field.length() != null) {
            column.setLength(field.length());
        }
        if (field.precision() != null) {
            column.setPrecision(field.precision());
        }
        if (field.scale() != null) {
            column.setScale(field.scale());
        }
        return column;
    }
}
