package net.ximatai.muyun.spring.platform.metadata;

/** Input for creating a dynamic module's primary entity. */
public record ModuleMainMetadataCreateCommand(
        String alias,
        String title,
        String schemaName,
        String tableName,
        Boolean dataScopeEnabled) {
}
