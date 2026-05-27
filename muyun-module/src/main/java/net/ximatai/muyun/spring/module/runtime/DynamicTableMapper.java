package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.builder.Column;
import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.schema.PlatformTableValidator;
import net.ximatai.muyun.spring.common.schema.PlatformUniqueIndexes;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinitionValidator;

public class DynamicTableMapper {
    private final ModuleDefinitionValidator validator = new ModuleDefinitionValidator();
    private final PlatformTableValidator tableValidator = new PlatformTableValidator();

    public TableWrapper toTable(EntityDefinition entity) {
        validator.validateEntity(entity);
        TableWrapper table = TableWrapper.withName(entity.tableName())
                .setComment(entity.name())
                .setPrimaryKey(StandardEntitySchema.idColumn());
        StandardEntitySchema.auditColumns().forEach(table::addColumn);
        for (FieldDefinition field : entity.fields()) {
            table.addColumn(toColumn(field));
            if (field.isUnique()) {
                PlatformUniqueIndexes.addTenantUniqueIndex(table, field.columnName());
            }
        }
        tableValidator.requireStandardEntityTable(table, entity.code());
        return table;
    }

    private Column toColumn(FieldDefinition field) {
        Column column = Column.of(field.columnName())
                .setType(field.type().toColumnType())
                .setComment(field.name())
                .setNullable(!field.isRequired())
                .setIndexed(field.isIndexed() || field.isSortable());
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
