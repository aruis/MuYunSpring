package net.ximatai.muyun.spring.platform.metadata;

/** Configuration records whose removal is protected from dangling references. */
public enum ConfigurationReferenceTarget {
    METADATA("metadataId", MetadataService.MODULE_ALIAS, "元数据"),
    METADATA_FIELD("metadataFieldId", MetadataFieldService.MODULE_ALIAS, "字段"),
    MODULE_METADATA_FIELD("moduleMetadataFieldId", ModuleMetadataFieldService.MODULE_ALIAS, "模块字段"),
    MODULE_METADATA_RELATION("moduleMetadataRelationId", ModuleMetadataRelationService.MODULE_ALIAS, "模块元数据关系");

    private final String detailKey;
    private final String moduleAlias;
    private final String resourceName;

    ConfigurationReferenceTarget(String detailKey, String moduleAlias, String resourceName) {
        this.detailKey = detailKey;
        this.moduleAlias = moduleAlias;
        this.resourceName = resourceName;
    }

    public String detailKey() { return detailKey; }
    public String moduleAlias() { return moduleAlias; }
    public String resourceName() { return resourceName; }
}
