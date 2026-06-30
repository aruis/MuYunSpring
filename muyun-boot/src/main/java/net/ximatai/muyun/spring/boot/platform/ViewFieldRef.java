package net.ximatai.muyun.spring.boot.platform;

public record ViewFieldRef(String relationCode,
                           String fieldName,
                           String fieldId) {
    public ViewFieldRef {
        relationCode = relationCode == null || relationCode.isBlank() ? null : relationCode.trim();
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("view field name must not be blank");
        }
        fieldName = fieldName.trim();
        fieldId = fieldId == null || fieldId.isBlank() ? null : fieldId.trim();
    }

    public static ViewFieldRef main(String fieldName) {
        return new ViewFieldRef(null, fieldName, null);
    }

    public static ViewFieldRef relation(String relationCode, String fieldName) {
        return new ViewFieldRef(relationCode, fieldName, null);
    }
}
