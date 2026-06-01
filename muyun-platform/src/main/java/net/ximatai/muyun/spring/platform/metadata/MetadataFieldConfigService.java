package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.FieldBehaviorDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldBehaviorSupport;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryService;
import org.springframework.stereotype.Service;

@Service
public class MetadataFieldConfigService extends AbstractAbilityService<MetadataFieldConfig> implements
        SoftDeleteAbility<MetadataFieldConfig> {
    public static final String MODULE_ALIAS = "platform.metadataFieldConfig";

    private final MetadataFieldService fieldService;
    private final MetadataService metadataService;
    private final PlatformFieldTypeService fieldTypeService;
    private final DictionaryCategoryService categoryService;
    private final ModuleMetadataRelationService relationService;

    public MetadataFieldConfigService(BaseDao<MetadataFieldConfig, String> configDao,
                                      MetadataFieldService fieldService,
                                      MetadataService metadataService,
                                      PlatformFieldTypeService fieldTypeService,
                                      DictionaryCategoryService categoryService,
                                      ModuleMetadataRelationService relationService) {
        super(MODULE_ALIAS, MetadataFieldConfig.class, configDao);
        this.fieldService = fieldService;
        this.metadataService = metadataService;
        this.fieldTypeService = fieldTypeService;
        this.categoryService = categoryService;
        this.relationService = relationService;
    }

    @Override
    public void beforeInsert(MetadataFieldConfig config) {
        normalizeAndValidate(config);
    }

    @Override
    public void beforeUpdate(MetadataFieldConfig config) {
        normalizeAndValidate(config);
    }

    public MetadataFieldConfig findByMetadataFieldId(String metadataFieldId) {
        if (metadataFieldId == null || metadataFieldId.isBlank()) {
            return null;
        }
        return findOne(Criteria.of()
                .eq("metadataFieldId", metadataFieldId)
                .isNull("relationId"));
    }

    public MetadataFieldConfig findRelationOverride(String metadataFieldId, String relationId) {
        if (metadataFieldId == null || metadataFieldId.isBlank()) {
            return null;
        }
        if (relationId != null && !relationId.isBlank()) {
            return findOne(Criteria.of()
                    .eq("metadataFieldId", metadataFieldId)
                    .eq("relationId", relationId));
        }
        return null;
    }

    private void normalizeAndValidate(MetadataFieldConfig config) {
        MetadataField field = requireField(config.getMetadataFieldId());
        normalizeRelation(config, field);
        PlatformFieldType fieldType = fieldTypeService.requireFieldType(field.getFieldTypeAlias());
        normalizeFieldShape(config, fieldType);
        normalizeDictionaryBinding(config, field, fieldType);
        normalizeQueryDefinition(config, fieldType);
        normalizeBehavior(config, fieldType);
        rejectDuplicate(config, scopeCriteria(config.getMetadataFieldId(), config.getRelationId()),
                "metadata field config must be unique in scope: " + config.getMetadataFieldId());
    }

    private Criteria scopeCriteria(String metadataFieldId, String relationId) {
        Criteria criteria = Criteria.of().eq("metadataFieldId", metadataFieldId);
        if (relationId == null || relationId.isBlank()) {
            return criteria.isNull("relationId");
        }
        return criteria.eq("relationId", relationId);
    }

    private void normalizeRelation(MetadataFieldConfig config, MetadataField field) {
        if (config.getRelationId() == null || config.getRelationId().isBlank()) {
            config.setRelationId(null);
            return;
        }
        ModuleMetadataRelation relation = relationService.select(config.getRelationId());
        if (relation == null) {
            throw new PlatformException("Field config requires existing relation: " + config.getRelationId());
        }
        if (!field.getMetadataId().equals(relation.getMetadataId())) {
            throw new PlatformException("Field config relation metadata mismatch: " + config.getRelationId());
        }
    }

    private void normalizeFieldShape(MetadataFieldConfig config, PlatformFieldType fieldType) {
        if (config.getRelationId() != null
                && (config.getFieldLength() != null || config.getPrecision() != null || config.getScale() != null)) {
            throw new PlatformException("Relation field config cannot override physical field shape: "
                    + config.getMetadataFieldId());
        }
        FieldShapeRules.validate(fieldType.getFieldType(), config.effectiveLength(fieldType),
                config.effectivePrecision(fieldType), config.effectiveScale(fieldType), config.getMetadataFieldId());
    }

    private void normalizeDictionaryBinding(MetadataFieldConfig config,
                                            MetadataField field,
                                            PlatformFieldType fieldType) {
        boolean hasCategory = config.getDictionaryCategoryAlias() != null && !config.getDictionaryCategoryAlias().isBlank();
        boolean hasApplication = config.getDictionaryApplicationAlias() != null && !config.getDictionaryApplicationAlias().isBlank();
        if (!hasCategory && !hasApplication) {
            config.setDictionaryApplicationAlias(null);
            config.setDictionaryCategoryAlias(null);
            return;
        }
        if (!hasCategory) {
            throw new IllegalArgumentException("dictionaryCategoryAlias must not be blank");
        }
        if (fieldType.getFieldType() != FieldType.STRING && fieldType.getFieldType() != FieldType.TEXT) {
            throw new IllegalArgumentException("dictionary binding requires string field");
        }
        Metadata metadata = metadataService.select(field.getMetadataId());
        if (metadata == null) {
            throw new PlatformException("Metadata field requires existing metadata: " + field.getMetadataId());
        }
        String applicationAlias = hasApplication
                ? PlatformNameRules.requireApplicationAlias(config.getDictionaryApplicationAlias())
                : metadata.getApplicationAlias();
        config.setDictionaryApplicationAlias(applicationAlias);
        config.setDictionaryCategoryAlias(PlatformNameRules.requireIdentifier(
                config.getDictionaryCategoryAlias(), "dictionaryCategoryAlias"));
        categoryService.requireDictionaryCategory(config.getDictionaryApplicationAlias(), config.getDictionaryCategoryAlias());
    }

    private void normalizeQueryDefinition(MetadataFieldConfig config, PlatformFieldType fieldType) {
        if (config.getQueryable() == null) {
            config.setDefaultQueryOperator(null);
            config.setQueryOperators(null);
            return;
        }
        if (!config.getQueryable()) {
            config.setDefaultQueryOperator(null);
            config.setQueryOperators(null);
            return;
        }
        if (config.getDefaultQueryOperator() == null) {
            config.setDefaultQueryOperator(DynamicQueryOperator.defaultOperator(fieldType.getFieldType()));
        }
        if (config.getQueryOperators() == null || config.getQueryOperators().isBlank()) {
            config.setQueryOperators(DynamicQueryOperator.format(DynamicQueryOperator.defaultOperators(fieldType.getFieldType())));
        }
        config.queryDefinition(fieldType);
    }

    private void normalizeBehavior(MetadataFieldConfig config, PlatformFieldType fieldType) {
        if (config.getDefaultValue() != null && config.getDefaultValue().isBlank()) {
            config.setDefaultValue(null);
        }
        if (config.getValidationRegex() != null && config.getValidationRegex().isBlank()) {
            config.setValidationRegex(null);
        }
        FieldBehaviorSupport.validateBehavior(
                fieldType.getFieldType(),
                new FieldBehaviorDefinition(config.getDefaultValue(), config.getValidationRegex(),
                        config.getCopyable() == null || Boolean.TRUE.equals(config.getCopyable()),
                        Boolean.TRUE.equals(config.getWriteProtected())),
                config.getMetadataFieldId()
        );
    }

    private MetadataField requireField(String metadataFieldId) {
        MetadataField field = metadataFieldId == null || metadataFieldId.isBlank() ? null : fieldService.select(metadataFieldId);
        if (field == null) {
            throw new PlatformException("Field config requires existing metadata field: " + metadataFieldId);
        }
        return field;
    }
}
