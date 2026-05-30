package net.ximatai.muyun.spring.dynamic.metadata;

public record EntityViewFieldDefinition(
        String fieldName,
        String title,
        boolean visible,
        ViewControlType controlType
) {
    public EntityViewFieldDefinition(String fieldName) {
        this(fieldName, null, true, null);
    }

    public EntityViewFieldDefinition hidden() {
        return new EntityViewFieldDefinition(fieldName, title, false, controlType);
    }

    public EntityViewFieldDefinition title(String value) {
        return new EntityViewFieldDefinition(fieldName, value, visible, controlType);
    }

    public EntityViewFieldDefinition control(ViewControlType value) {
        return new EntityViewFieldDefinition(fieldName, title, visible, value);
    }
}
