package net.ximatai.muyun.spring.dynamic.metadata;

public record EntityViewFieldDefinition(
        String fieldName,
        String title,
        boolean visible,
        ViewControlType controlType,
        Boolean readOnly,
        Boolean required
) {
    public EntityViewFieldDefinition(String fieldName) {
        this(fieldName, null, true, null, null, null);
    }

    public EntityViewFieldDefinition(String fieldName, String title, boolean visible, ViewControlType controlType) {
        this(fieldName, title, visible, controlType, null, null);
    }

    public EntityViewFieldDefinition hidden() {
        return new EntityViewFieldDefinition(fieldName, title, false, controlType, readOnly, required);
    }

    public EntityViewFieldDefinition title(String value) {
        return new EntityViewFieldDefinition(fieldName, value, visible, controlType, readOnly, required);
    }

    public EntityViewFieldDefinition control(ViewControlType value) {
        return new EntityViewFieldDefinition(fieldName, title, visible, value, readOnly, required);
    }

    public EntityViewFieldDefinition readOnly(boolean value) {
        return new EntityViewFieldDefinition(fieldName, title, visible, controlType, value, required);
    }

    public EntityViewFieldDefinition required(boolean value) {
        return new EntityViewFieldDefinition(fieldName, title, visible, controlType, readOnly, value);
    }
}
