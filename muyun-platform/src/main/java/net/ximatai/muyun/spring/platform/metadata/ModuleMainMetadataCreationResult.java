package net.ximatai.muyun.spring.platform.metadata;

/** The entity and its primary module binding created as one orchestration operation. */
public record ModuleMainMetadataCreationResult(
        Metadata metadata,
        ModuleMetadataRelation relation) {
}
