package net.ximatai.muyun.spring.dynamic.metadata;

public record EntityViewFieldDefinition(
        String fieldName,
        String title,
        boolean visible,
        ViewControlType controlType,
        String fieldUiTypeAlias,
        Boolean readOnly,
        Boolean required
) {
    public EntityViewFieldDefinition(String fieldName) {
        this(fieldName, null, true, null, null, null, null);
    }

    public EntityViewFieldDefinition(String fieldName, String title, boolean visible, ViewControlType controlType) {
        this(fieldName, title, visible, controlType, null, null, null);
    }

    public EntityViewFieldDefinition hidden() {
        return new EntityViewFieldDefinition(fieldName, title, false, controlType, fieldUiTypeAlias, readOnly, required);
    }

    public EntityViewFieldDefinition title(String value) {
        return new EntityViewFieldDefinition(fieldName, value, visible, controlType, fieldUiTypeAlias, readOnly, required);
    }

    public EntityViewFieldDefinition control(ViewControlType value) {
        return new EntityViewFieldDefinition(fieldName, title, visible, value, fieldUiTypeAlias, readOnly, required);
    }

    public EntityViewFieldDefinition fieldUiType(String value) {
        return new EntityViewFieldDefinition(fieldName, title, visible, controlType, value, readOnly, required);
    }

    public EntityViewFieldDefinition readOnly(boolean value) {
        return new EntityViewFieldDefinition(fieldName, title, visible, controlType, fieldUiTypeAlias, value, required);
    }

    public EntityViewFieldDefinition required(boolean value) {
        return new EntityViewFieldDefinition(fieldName, title, visible, controlType, fieldUiTypeAlias, readOnly, value);
    }
}
