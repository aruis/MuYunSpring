package net.ximatai.muyun.spring.dynamic.metadata;

public record FieldCompanionDefinition(
        String ownerField,
        String fieldName,
        String columnName,
        FieldType type,
        FieldCompanionRole role,
        boolean requiredWhenOwnerPresent,
        boolean requiredWhenOwnerUpdated
) {
}
