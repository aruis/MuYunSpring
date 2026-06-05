package net.ximatai.muyun.spring.dynamic.schema;

import net.ximatai.muyun.database.core.builder.Column;
import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.schema.PlatformTableValidator;
import net.ximatai.muyun.spring.common.schema.PlatformUniqueIndexes;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicAbilityFields;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldCompanionRules;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionValidator;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DynamicTableMapper {
    private final ModuleDefinitionValidator validator = new ModuleDefinitionValidator();
    private final PlatformTableValidator tableValidator = new PlatformTableValidator();

    public TableWrapper toTable(EntityDefinition entity) {
        validator.validateEntity(entity);
        TableWrapper table = TableWrapper.withName(entity.tableName())
                .setSchema(entity.schemaName())
                .setComment(entity.name())
                .setPrimaryKey(StandardEntitySchema.idColumn());
        StandardEntitySchema.auditColumns().forEach(table::addColumn);
        for (FieldDefinition field : tableFields(entity)) {
            table.addColumn(toColumn(field));
            if (field.isUnique()) {
                PlatformUniqueIndexes.addTenantUniqueIndex(table, field.columnName());
            }
        }
        tableValidator.requireStandardEntityTable(table, entity.alias());
        return table;
    }

    public TableWrapper toTable(EntityDefinition entity, EntityDefinition previousEntity) {
        TableWrapper table = toTable(entity);
        if (previousEntity == null
                || !java.util.Objects.equals(entity.schemaName(), previousEntity.schemaName())
                || !entity.tableName().equals(previousEntity.tableName())) {
            return table;
        }
        validator.validateEntity(previousEntity);
        Set<String> targetColumns = tableFields(entity).stream()
                .map(FieldDefinition::columnName)
                .collect(Collectors.toSet());
        tableFields(previousEntity).stream()
                .map(FieldDefinition::columnName)
                .filter(column -> !targetColumns.contains(column))
                .forEach(table::dropColumn);
        return table;
    }

    private List<FieldDefinition> tableFields(EntityDefinition entity) {
        java.util.ArrayList<FieldDefinition> fields = new java.util.ArrayList<>();
        if (entity.supports(EntityCapability.DATA_SCOPE)) {
            fields.addAll(DynamicAbilityFields.dataScopeFields());
        }
        if (entity.supports(EntityCapability.APPROVAL)) {
            fields.addAll(DynamicAbilityFields.approvalFields());
        }
        fields.addAll(entity.fields());
        fields.addAll(FieldCompanionRules.generatedFields(entity));
        return List.copyOf(fields);
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
