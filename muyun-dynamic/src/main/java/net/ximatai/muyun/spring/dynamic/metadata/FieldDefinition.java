package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;

public record FieldDefinition(
        String fieldName,
        String columnName,
        FieldType type,
        String name,
        boolean isRequired,
        boolean isUnique,
        boolean isIndexed,
        boolean isSortable,
        boolean isTitle,
        Integer length,
        Integer precision,
        Integer scale
) {
    public FieldDefinition(String fieldName, String columnName, FieldType type, String name) {
        this(fieldName, columnName, type, name, false, false, false, false, false, null, null, null);
    }

    public static FieldDefinition of(String fieldName, FieldType type, String name) {
        return new FieldDefinition(fieldName, fieldName, type, name);
    }

    public static FieldDefinition string(String fieldName, String name) {
        return of(fieldName, FieldType.STRING, name);
    }

    public static FieldDefinition text(String fieldName, String name) {
        return of(fieldName, FieldType.TEXT, name);
    }

    public static FieldDefinition integer(String fieldName, String name) {
        return of(fieldName, FieldType.INTEGER, name);
    }

    public static FieldDefinition longInteger(String fieldName, String name) {
        return of(fieldName, FieldType.LONG, name);
    }

    public static FieldDefinition decimal(String fieldName, String name) {
        return of(fieldName, FieldType.DECIMAL, name);
    }

    public static FieldDefinition bool(String fieldName, String name) {
        return of(fieldName, FieldType.BOOLEAN, name);
    }

    public static FieldDefinition timestamp(String fieldName, String name) {
        return of(fieldName, FieldType.TIMESTAMP, name);
    }

    public static FieldDefinition parentId() {
        return string(PlatformAbilityFields.TREE_PARENT_FIELD, "Parent")
                .column(PlatformAbilityFields.TREE_PARENT_COLUMN)
                .length(PlatformAbilityFields.TREE_PARENT_LENGTH);
    }

    public static FieldDefinition sortOrder() {
        return integer(PlatformAbilityFields.SORT_FIELD, "Sort Order")
                .column(PlatformAbilityFields.SORT_COLUMN)
                .sortable();
    }

    public static FieldDefinition titleField() {
        return string(PlatformAbilityFields.TITLE_FIELD, "Title")
                .column(PlatformAbilityFields.TITLE_COLUMN)
                .length(PlatformAbilityFields.TITLE_LENGTH)
                .title();
    }

    public static FieldDefinition enabled() {
        return bool(PlatformAbilityFields.ENABLED_FIELD, "Enabled")
                .column(PlatformAbilityFields.ENABLED_COLUMN);
    }

    public String code() {
        return fieldName;
    }

    public FieldDefinition column(String value) {
        return new FieldDefinition(fieldName, value, type, name, isRequired, isUnique, isIndexed, isSortable, isTitle, length, precision, scale);
    }

    public FieldDefinition required() {
        return new FieldDefinition(fieldName, columnName, type, name, true, isUnique, isIndexed, isSortable, isTitle, length, precision, scale);
    }

    public FieldDefinition unique() {
        return new FieldDefinition(fieldName, columnName, type, name, isRequired, true, isIndexed, isSortable, isTitle, length, precision, scale);
    }

    public FieldDefinition indexed() {
        return new FieldDefinition(fieldName, columnName, type, name, isRequired, isUnique, true, isSortable, isTitle, length, precision, scale);
    }

    public FieldDefinition sortable() {
        return new FieldDefinition(fieldName, columnName, type, name, isRequired, isUnique, isIndexed, true, isTitle, length, precision, scale);
    }

    public FieldDefinition title() {
        return new FieldDefinition(fieldName, columnName, type, name, isRequired, isUnique, isIndexed, isSortable, true, length, precision, scale);
    }

    public FieldDefinition length(int value) {
        return new FieldDefinition(fieldName, columnName, type, name, isRequired, isUnique, isIndexed, isSortable, isTitle, value, precision, scale);
    }

    public FieldDefinition precision(int value, int scaleValue) {
        return new FieldDefinition(fieldName, columnName, type, name, isRequired, isUnique, isIndexed, isSortable, isTitle, length, value, scaleValue);
    }
}
