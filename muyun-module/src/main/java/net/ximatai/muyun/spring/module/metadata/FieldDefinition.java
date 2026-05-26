package net.ximatai.muyun.spring.module.metadata;

public record FieldDefinition(
        String code,
        String columnName,
        FieldType type,
        String name,
        boolean required,
        boolean unique,
        boolean indexed,
        boolean sortable,
        Integer length,
        Integer precision,
        Integer scale
) {
    public FieldDefinition(String code, String columnName, FieldType type, String name) {
        this(code, columnName, type, name, false, false, false, false, null, null, null);
    }

    public FieldDefinition asRequired() {
        return new FieldDefinition(code, columnName, type, name, true, unique, indexed, sortable, length, precision, scale);
    }

    public FieldDefinition asUnique() {
        return new FieldDefinition(code, columnName, type, name, required, true, indexed, sortable, length, precision, scale);
    }

    public FieldDefinition asIndexed() {
        return new FieldDefinition(code, columnName, type, name, required, unique, true, sortable, length, precision, scale);
    }

    public FieldDefinition asSortable() {
        return new FieldDefinition(code, columnName, type, name, required, unique, true, true, length, precision, scale);
    }

    public FieldDefinition length(int value) {
        return new FieldDefinition(code, columnName, type, name, required, unique, indexed, sortable, value, precision, scale);
    }

    public FieldDefinition precision(int value, int scaleValue) {
        return new FieldDefinition(code, columnName, type, name, required, unique, indexed, sortable, length, value, scaleValue);
    }
}
