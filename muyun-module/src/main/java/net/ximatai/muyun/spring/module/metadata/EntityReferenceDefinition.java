package net.ximatai.muyun.spring.module.metadata;

public record EntityReferenceDefinition(
        String sourceEntity,
        String sourceField,
        String targetReferenceNamespace
) {
    public static EntityReferenceDefinition from(String sourceEntity, String sourceField, String targetReferenceNamespace) {
        return new EntityReferenceDefinition(sourceEntity, sourceField, targetReferenceNamespace);
    }
}
