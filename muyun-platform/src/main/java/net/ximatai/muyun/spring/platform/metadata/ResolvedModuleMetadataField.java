package net.ximatai.muyun.spring.platform.metadata;

public record ResolvedModuleMetadataField(
        String moduleMetadataFieldId,
        String moduleAlias,
        String relationId,
        String relationAlias,
        RelationRole relationRole,
        String metadataId,
        String metadataAlias,
        String metadataTitle,
        String metadataFieldId,
        String fieldName,
        String columnName,
        String fieldTitle,
        String fieldTypeAlias
) {
}
