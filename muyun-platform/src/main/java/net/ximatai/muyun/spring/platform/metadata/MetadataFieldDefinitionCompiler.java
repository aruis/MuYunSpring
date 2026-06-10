package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldBehaviorDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldQueryDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetadataFieldDefinitionCompiler {
    private final PlatformFieldTypeService fieldTypeService;
    private final MetadataFieldConfigService configService;
    private final MetadataFieldProtectionConfigService protectionConfigService;

    public MetadataFieldDefinitionCompiler(PlatformFieldTypeService fieldTypeService,
                                           MetadataFieldConfigService configService) {
        this(fieldTypeService, configService, null);
    }

    @Autowired
    public MetadataFieldDefinitionCompiler(PlatformFieldTypeService fieldTypeService,
                                           MetadataFieldConfigService configService,
                                           MetadataFieldProtectionConfigService protectionConfigService) {
        this.fieldTypeService = fieldTypeService;
        this.configService = configService;
        this.protectionConfigService = protectionConfigService;
    }

    public FieldDefinition compile(MetadataField field) {
        return compile(field, null);
    }

    public FieldDefinition compile(MetadataField field, String relationId) {
        return compile(field, relationId, null);
    }

    public FieldDefinition compile(MetadataField field, String relationId, ModuleMetadataField moduleField) {
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
        boolean hasModuleDictionary = moduleField != null
                && moduleField.getDictionaryCategoryAlias() != null
                && !moduleField.getDictionaryCategoryAlias().isBlank();
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
                queryDefinition,
                fieldType.getDefaultUiTypeAlias(),
                behavior(fieldType, defaultConfig, relationConfig, moduleField, field.getId()),
                protectionConfigService == null
                        ? net.ximatai.muyun.spring.common.security.FieldProtectionDefinition.NONE
                        : protectionConfigService.definition(field.getId())
        );
        if (hasModuleDictionary) {
            validateModuleDictionary(fieldType, moduleField, field.getId());
            definition = definition.dictionary(moduleField.getDictionaryApplicationAlias(),
                    moduleField.getDictionaryCategoryAlias());
        } else if (dictionaryConfig != null && dictionaryConfig.hasDictionaryBinding()) {
            definition = definition.dictionary(dictionaryConfig.getDictionaryApplicationAlias(),
                    dictionaryConfig.getDictionaryCategoryAlias(),
                    dictionaryConfig.getSelectionMode());
        }
        return definition;
    }

    private FieldBehaviorDefinition behavior(PlatformFieldType fieldType,
                                             MetadataFieldConfig defaultConfig,
                                             MetadataFieldConfig relationConfig,
                                             String fieldId) {
        if (defaultConfig == null && relationConfig == null) {
            return FieldBehaviorDefinition.DEFAULT;
        }
        String defaultValue = relationConfig != null && relationConfig.getDefaultValue() != null
                ? relationConfig.getDefaultValue()
                : defaultConfig == null ? null : defaultConfig.getDefaultValue();
        String validationRegex = relationConfig != null && relationConfig.getValidationRegex() != null
                ? relationConfig.getValidationRegex()
                : defaultConfig == null ? null : defaultConfig.getValidationRegex();
        boolean copyable = relationConfig != null && relationConfig.getCopyable() != null
                ? Boolean.TRUE.equals(relationConfig.getCopyable())
                : defaultConfig == null || defaultConfig.getCopyable() == null || Boolean.TRUE.equals(defaultConfig.getCopyable());
        boolean writeProtected = relationConfig != null && relationConfig.getWriteProtected() != null
                ? Boolean.TRUE.equals(relationConfig.getWriteProtected())
                : defaultConfig != null && Boolean.TRUE.equals(defaultConfig.getWriteProtected());
        FieldBehaviorDefinition behavior = new FieldBehaviorDefinition(
                defaultValue,
                validationRegex,
                copyable,
                writeProtected
        );
        net.ximatai.muyun.spring.dynamic.metadata.FieldBehaviorSupport.validateBehavior(
                fieldType.getFieldType(), behavior, fieldId);
        return behavior;
    }

    private FieldBehaviorDefinition behavior(PlatformFieldType fieldType,
                                             MetadataFieldConfig defaultConfig,
                                             MetadataFieldConfig relationConfig,
                                             ModuleMetadataField moduleField,
                                             String fieldId) {
        FieldBehaviorDefinition inherited = behavior(fieldType, defaultConfig, relationConfig, fieldId);
        if (moduleField == null) {
            return inherited;
        }
        String defaultValue = moduleField.getDefaultValue() != null
                ? moduleField.getDefaultValue()
                : inherited.defaultValue();
        String validationRegex = moduleField.getValidationRegex() != null
                ? moduleField.getValidationRegex()
                : inherited.validationRegex();
        boolean copyable = moduleField.getCloneable() == null
                ? inherited.copyable()
                : Boolean.TRUE.equals(moduleField.getCloneable());
        FieldBehaviorDefinition behavior = new FieldBehaviorDefinition(
                defaultValue,
                validationRegex,
                copyable,
                inherited.writeProtected()
        );
        net.ximatai.muyun.spring.dynamic.metadata.FieldBehaviorSupport.validateBehavior(
                fieldType.getFieldType(), behavior, fieldId);
        return behavior;
    }

    private void validateModuleDictionary(PlatformFieldType fieldType,
                                          ModuleMetadataField moduleField,
                                          String fieldId) {
        FieldType type = fieldType.getFieldType();
        if (type != FieldType.STRING && type != FieldType.TEXT) {
            throw new IllegalArgumentException("module field dictionary binding requires string field: " + fieldId);
        }
        if (moduleField.getDictionaryApplicationAlias() == null
                || moduleField.getDictionaryApplicationAlias().isBlank()) {
            throw new IllegalArgumentException("module field dictionaryApplicationAlias must not be blank: " + fieldId);
        }
    }
}
