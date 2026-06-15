package net.ximatai.muyun.spring.platform.metadata;

public record ModuleMetadataMeasureUnitPrepareResult(
        ModuleMetadataField moduleField,
        MetadataField unitField,
        MetadataField baseValueField
) {
}
