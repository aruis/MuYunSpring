package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldQueryDefinition;
import org.springframework.stereotype.Service;

@Service
public class MetadataFieldDefinitionCompiler {
    private final PlatformFieldTypeService fieldTypeService;
    private final MetadataFieldConfigService configService;

    public MetadataFieldDefinitionCompiler(PlatformFieldTypeService fieldTypeService,
                                           MetadataFieldConfigService configService) {
        this.fieldTypeService = fieldTypeService;
        this.configService = configService;
    }

    public FieldDefinition compile(MetadataField field) {
        return compile(field, null);
    }

    public FieldDefinition compile(MetadataField field, String relationId) {
        PlatformFieldType fieldType = fieldTypeService.requireFieldType(field.getFieldTypeAlias());
        MetadataFieldConfig defaultConfig = configService.findByMetadataFieldId(field.getId());
        MetadataFieldConfig relationConfig = configService.findRelationOverride(field.getId(), relationId);
        MetadataFieldConfig queryConfig = relationConfig != null && relationConfig.getQueryable() != null
                ? relationConfig
                : defaultConfig;
        MetadataFieldConfig shapeConfig = defaultConfig;
        MetadataFieldConfig dictionaryConfig = relationConfig != null && relationConfig.hasDictionaryBinding()
                ? relationConfig
                : defaultConfig;
        FieldQueryDefinition queryDefinition = queryConfig == null
                ? fieldType.queryDefinition()
                : queryConfig.queryDefinition(fieldType);
        Integer length = shapeConfig == null ? fieldType.getDefaultLength() : shapeConfig.effectiveLength(fieldType);
        Integer precision = shapeConfig == null ? fieldType.getDefaultPrecision() : shapeConfig.effectivePrecision(fieldType);
        Integer scale = shapeConfig == null ? fieldType.getDefaultScale() : shapeConfig.effectiveScale(fieldType);
        FieldDefinition definition = new FieldDefinition(
                field.getFieldName(),
                field.getColumnName(),
                fieldType.getFieldType(),
                field.getTitle(),
                Boolean.TRUE.equals(field.getRequired()),
                Boolean.TRUE.equals(field.getUniqueField()),
                Boolean.TRUE.equals(field.getIndexed()),
                Boolean.TRUE.equals(field.getSortableField()),
                Boolean.TRUE.equals(field.getTitleField()),
                length,
                precision,
                scale,
                null,
                queryDefinition
        );
        if (dictionaryConfig != null && dictionaryConfig.hasDictionaryBinding()) {
            definition = definition.dictionary(dictionaryConfig.getDictionaryApplicationAlias(),
                    dictionaryConfig.getDictionaryCategoryAlias());
        }
        return definition;
    }
}
