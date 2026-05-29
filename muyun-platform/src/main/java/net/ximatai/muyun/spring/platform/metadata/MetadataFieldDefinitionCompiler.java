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
        PlatformFieldType fieldType = fieldTypeService.requireFieldType(field.getFieldTypeAlias());
        MetadataFieldConfig config = configService.findByMetadataFieldId(field.getId());
        FieldQueryDefinition queryDefinition = config == null
                ? fieldType.queryDefinition()
                : config.queryDefinition(fieldType);
        Integer length = config == null ? fieldType.getDefaultLength() : config.effectiveLength(fieldType);
        Integer precision = config == null ? fieldType.getDefaultPrecision() : config.effectivePrecision(fieldType);
        Integer scale = config == null ? fieldType.getDefaultScale() : config.effectiveScale(fieldType);
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
        if (config != null && config.hasDictionaryBinding()) {
            definition = definition.dictionary(config.getDictionaryApplicationAlias(), config.getDictionaryCategoryAlias());
        }
        return definition;
    }
}
