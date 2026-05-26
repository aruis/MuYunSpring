package net.ximatai.muyun.spring.module.metadata;

import net.ximatai.muyun.spring.common.schema.StandardModelSchema;

import java.util.HashSet;
import java.util.Set;

public class ModuleDefinitionValidator {
    private static final String IDENTIFIER_PATTERN = "[a-z][a-z0-9_]{0,62}";
    private static final String MODULE_ALIAS_PATTERN = "[a-z][a-z0-9_]{0,62}(\\.[a-z][a-z0-9_]{0,62})+";
    private static final Set<String> STANDARD_COLUMNS = Set.copyOf(StandardModelSchema.columnNames());

    public void validate(ModuleDefinition module) {
        if (module == null) {
            throw new ModuleDefinitionException("module must not be null");
        }
        requireModuleAlias(module.moduleAlias(), "module alias");
        requireText(module.name(), "module name");
        Set<String> entityCodes = new HashSet<>();
        Set<String> tableNames = new HashSet<>();
        for (EntityDefinition entity : module.entities()) {
            validateEntity(entity);
            requireUnique(entityCodes, entity.code(), "entity code");
            requireUnique(tableNames, entity.tableName(), "table name");
        }
    }

    public void validateEntity(EntityDefinition entity) {
        if (entity == null) {
            throw new ModuleDefinitionException("entity must not be null");
        }
        requireIdentifier(entity.code(), "entity code");
        requireIdentifier(entity.tableName(), "table name");
        requireText(entity.name(), "entity name");
        Set<String> fieldCodes = new HashSet<>();
        Set<String> columnNames = new HashSet<>();
        int sortableFields = 0;
        int titleFields = 0;
        for (FieldDefinition field : entity.fields()) {
            validateField(field);
            requireUnique(fieldCodes, field.code(), "field code");
            requireUnique(columnNames, field.columnName(), "column name");
            if (field.isSortable()) {
                sortableFields++;
            }
            if (field.isTitle()) {
                titleFields++;
            }
        }
        if (!entity.supports(EntityCapability.CRUD)) {
            throw new ModuleDefinitionException("dynamic entity requires CRUD capability: " + entity.code());
        }
        if (sortableFields > 1) {
            throw new ModuleDefinitionException("entity can only have one sortable field: " + entity.code());
        }
        if (titleFields > 1) {
            throw new ModuleDefinitionException("entity can only have one title field: " + entity.code());
        }
        if (sortableFields > 0 && !entity.supports(EntityCapability.SORT)) {
            throw new ModuleDefinitionException("sortable field requires SORT capability: " + entity.code());
        }
        if (entity.supports(EntityCapability.SORT) && sortableFields == 0) {
            throw new ModuleDefinitionException("SORT capability requires a sortable field: " + entity.code());
        }
        if (titleFields > 0 && !entity.supports(EntityCapability.REFERENCE)) {
            throw new ModuleDefinitionException("title field requires REFERENCE capability: " + entity.code());
        }
        if (entity.supports(EntityCapability.REFERENCE) && titleFields == 0) {
            throw new ModuleDefinitionException("REFERENCE capability requires a title field: " + entity.code());
        }
    }

    public void validateField(FieldDefinition field) {
        if (field == null) {
            throw new ModuleDefinitionException("field must not be null");
        }
        requireIdentifier(field.fieldName(), "field name");
        requireIdentifier(field.columnName(), "column name");
        requireText(field.name(), "field title");
        if (field.type() == null) {
            throw new ModuleDefinitionException("field type must not be null: " + field.code());
        }
        if (STANDARD_COLUMNS.contains(field.columnName())) {
            throw new ModuleDefinitionException("field column conflicts with standard column: " + field.columnName());
        }
        if (field.length() != null && field.length() <= 0) {
            throw new ModuleDefinitionException("field length must be positive: " + field.code());
        }
        if (field.length() != null && field.type() != FieldType.STRING && field.type() != FieldType.TEXT) {
            throw new ModuleDefinitionException("field length only applies to string fields: " + field.code());
        }
        if (field.precision() != null && field.precision() <= 0) {
            throw new ModuleDefinitionException("field precision must be positive: " + field.code());
        }
        if (field.scale() != null && field.scale() < 0) {
            throw new ModuleDefinitionException("field scale must not be negative: " + field.code());
        }
        if ((field.precision() != null || field.scale() != null) && field.type() != FieldType.DECIMAL) {
            throw new ModuleDefinitionException("field precision and scale only apply to decimal fields: " + field.code());
        }
        if (field.scale() != null && field.precision() == null) {
            throw new ModuleDefinitionException("field scale requires precision: " + field.code());
        }
        if (field.scale() != null && field.scale() > field.precision()) {
            throw new ModuleDefinitionException("field scale must not exceed precision: " + field.code());
        }
        if (field.isSortable() && field.type() != FieldType.INTEGER && field.type() != FieldType.LONG) {
            throw new ModuleDefinitionException("sortable field must be an integer type: " + field.code());
        }
        if (field.isTitle() && field.type() != FieldType.STRING && field.type() != FieldType.TEXT) {
            throw new ModuleDefinitionException("title field must be a text type: " + field.code());
        }
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new ModuleDefinitionException(name + " must not be blank");
        }
    }

    private void requireIdentifier(String value, String name) {
        requireText(value, name);
        if (!value.matches(IDENTIFIER_PATTERN)) {
            throw new ModuleDefinitionException("invalid " + name + ": " + value);
        }
    }

    private void requireModuleAlias(String value, String name) {
        requireText(value, name);
        if (!value.matches(MODULE_ALIAS_PATTERN)) {
            throw new ModuleDefinitionException("invalid " + name + ": " + value);
        }
    }

    private void requireUnique(Set<String> values, String value, String name) {
        if (!values.add(value)) {
            throw new ModuleDefinitionException("duplicate " + name + ": " + value);
        }
    }
}
