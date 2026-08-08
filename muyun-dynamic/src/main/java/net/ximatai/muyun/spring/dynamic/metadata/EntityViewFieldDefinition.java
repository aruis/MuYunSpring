package net.ximatai.muyun.spring.dynamic.metadata;

public record EntityViewFieldDefinition(
        String fieldName,
        String title,
        boolean visible,
        ViewControlType controlType,
        String fieldUiControlAlias,
        Boolean readOnly,
        Boolean required,
        Integer columnSpan
) {
    public EntityViewFieldDefinition {
        columnSpan = columnSpan == null ? 1 : requireColumnSpan(columnSpan);
    }

    public EntityViewFieldDefinition(String fieldName) {
        this(fieldName, null, true, null, null, null, null, 1);
    }

    public EntityViewFieldDefinition(String fieldName, String title, boolean visible, ViewControlType controlType) {
        this(fieldName, title, visible, controlType, null, null, null, 1);
    }

    public EntityViewFieldDefinition hidden() {
        return new EntityViewFieldDefinition(fieldName, title, false, controlType, fieldUiControlAlias, readOnly, required, columnSpan);
    }

    public EntityViewFieldDefinition title(String value) {
        return new EntityViewFieldDefinition(fieldName, value, visible, controlType, fieldUiControlAlias, readOnly, required, columnSpan);
    }

    public EntityViewFieldDefinition control(ViewControlType value) {
        return new EntityViewFieldDefinition(fieldName, title, visible, value, fieldUiControlAlias, readOnly, required, columnSpan);
    }

    public EntityViewFieldDefinition fieldUiType(String value) {
        return new EntityViewFieldDefinition(fieldName, title, visible, controlType, value, readOnly, required, columnSpan);
    }

    public EntityViewFieldDefinition readOnly(boolean value) {
        return new EntityViewFieldDefinition(fieldName, title, visible, controlType, fieldUiControlAlias, value, required, columnSpan);
    }

    public EntityViewFieldDefinition required(boolean value) {
        return new EntityViewFieldDefinition(fieldName, title, visible, controlType, fieldUiControlAlias, readOnly, value, columnSpan);
    }

    public EntityViewFieldDefinition columnSpan(int value) {
        return new EntityViewFieldDefinition(fieldName, title, visible, controlType, fieldUiControlAlias, readOnly, required, value);
    }

    private static int requireColumnSpan(int value) {
        if (value < 1 || value > 2) {
            throw new IllegalArgumentException("columnSpan must be between 1 and 2");
        }
        return value;
    }
}
