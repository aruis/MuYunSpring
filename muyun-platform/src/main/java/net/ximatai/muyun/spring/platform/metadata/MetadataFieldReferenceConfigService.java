package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.reference.ReferenceProjection;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class MetadataFieldReferenceConfigService extends AbstractAbilityService<MetadataFieldReferenceConfig> implements
        SoftDeleteAbility<MetadataFieldReferenceConfig> {
    public static final String MODULE_ALIAS = "platform.metadata_field_reference_config";
    private static final Set<String> STANDARD_FIELDS = Set.copyOf(StandardEntitySchema.fieldNames());

    private final MetadataFieldService fieldService;
    private final MetadataService metadataService;
    private final PlatformFieldTypeService fieldTypeService;
    private final PlatformModuleService moduleService;
    private final ModuleMetadataRelationService relationService;

    public MetadataFieldReferenceConfigService(BaseDao<MetadataFieldReferenceConfig, String> referenceConfigDao,
                                               MetadataFieldService fieldService,
                                               MetadataService metadataService,
                                               PlatformFieldTypeService fieldTypeService,
                                               PlatformModuleService moduleService,
                                               ModuleMetadataRelationService relationService) {
        super(MODULE_ALIAS, MetadataFieldReferenceConfig.class, referenceConfigDao);
        this.fieldService = fieldService;
        this.metadataService = metadataService;
        this.fieldTypeService = fieldTypeService;
        this.moduleService = moduleService;
        this.relationService = relationService;
    }

    @Override
    public void beforeInsert(MetadataFieldReferenceConfig config) {
        normalizeAndValidate(config);
    }

    @Override
    public void beforeUpdate(MetadataFieldReferenceConfig config) {
        normalizeAndValidate(config);
    }

    public MetadataFieldReferenceConfig findByMetadataFieldId(String metadataFieldId) {
        if (metadataFieldId == null || metadataFieldId.isBlank()) {
            return null;
        }
        return findOne(Criteria.of()
                .eq("metadataFieldId", metadataFieldId)
                .isNull("relationId"));
    }

    public MetadataFieldReferenceConfig findForRelation(String metadataFieldId, String relationId) {
        if (metadataFieldId == null || metadataFieldId.isBlank()) {
            return null;
        }
        if (relationId != null && !relationId.isBlank()) {
            MetadataFieldReferenceConfig override = findOne(Criteria.of()
                    .eq("metadataFieldId", metadataFieldId)
                    .eq("relationId", relationId));
            if (override != null) {
                return override;
            }
        }
        return findByMetadataFieldId(metadataFieldId);
    }

    private void normalizeAndValidate(MetadataFieldReferenceConfig config) {
        MetadataField sourceField = requireField(config.getMetadataFieldId(), "source metadata field");
        normalizeRelation(config, sourceField);
        PlatformFieldType sourceType = fieldTypeService.requireFieldType(sourceField.getFieldTypeAlias());
        if (sourceType.getFieldType() != FieldType.STRING && sourceType.getFieldType() != FieldType.TEXT) {
            throw new IllegalArgumentException("reference source field must be string/text: " + sourceField.getFieldName());
        }
        Metadata targetMetadata = metadataService.select(config.getTargetMetadataId());
        if (targetMetadata == null) {
            throw new PlatformException("Reference config requires existing target metadata: " + config.getTargetMetadataId());
        }
        if (config.getTargetModuleAlias() != null && !config.getTargetModuleAlias().isBlank()) {
            String targetModuleAlias = PlatformNameRules.requireModuleAlias(config.getTargetModuleAlias());
            if (moduleService.select(targetModuleAlias) == null) {
                throw new PlatformException("Reference config requires existing target module: " + targetModuleAlias);
            }
            config.setTargetModuleAlias(targetModuleAlias);
        } else {
            config.setTargetModuleAlias(null);
        }
        if (config.getCardinality() == null) {
            config.setCardinality(ReferenceCardinality.ONE);
        }
        normalizeAutoTitle(config);
        validateOutputFields(config, sourceField.getMetadataId());
        if (config.getTargetModuleAlias() != null
                && (Boolean.TRUE.equals(config.getAutoTitle()) || !config.projections().isEmpty())) {
            throw new PlatformException("Cross-module reference display is not supported yet: " + config.getTargetModuleAlias());
        }
        rejectDuplicate(config, scopeCriteria(config.getMetadataFieldId(), config.getRelationId()),
                "metadata field reference config must be unique in scope: " + config.getMetadataFieldId());
    }

    private Criteria scopeCriteria(String metadataFieldId, String relationId) {
        Criteria criteria = Criteria.of().eq("metadataFieldId", metadataFieldId);
        if (relationId == null || relationId.isBlank()) {
            return criteria.isNull("relationId");
        }
        return criteria.eq("relationId", relationId);
    }

    private void normalizeRelation(MetadataFieldReferenceConfig config, MetadataField field) {
        if (config.getRelationId() == null || config.getRelationId().isBlank()) {
            config.setRelationId(null);
            return;
        }
        ModuleMetadataRelation relation = relationService.select(config.getRelationId());
        if (relation == null) {
            throw new PlatformException("Reference config requires existing relation: " + config.getRelationId());
        }
        if (!field.getMetadataId().equals(relation.getMetadataId())) {
            throw new PlatformException("Reference config relation metadata mismatch: " + config.getRelationId());
        }
    }

    private void normalizeAutoTitle(MetadataFieldReferenceConfig config) {
        if (config.getAutoTitle() == null) {
            config.setAutoTitle(Boolean.FALSE);
        }
        if (!config.getAutoTitle()) {
            config.setTitleOutputField(null);
            return;
        }
        if (config.getTitleOutputField() == null || config.getTitleOutputField().isBlank()) {
            config.setTitleOutputField(sourceFieldName(config) + "Title");
        }
        config.setTitleOutputField(PlatformNameRules.requireFieldName(config.getTitleOutputField(), "titleOutputField"));
    }

    private void validateOutputFields(MetadataFieldReferenceConfig config, String sourceMetadataId) {
        LinkedHashSet<String> outputFields = new LinkedHashSet<>();
        if (Boolean.TRUE.equals(config.getAutoTitle())) {
            requireAvailableOutputField(sourceMetadataId, config.getTitleOutputField(), "reference title output field");
            outputFields.add(config.getTitleOutputField());
        }
        for (ReferenceProjection projection : config.projections()) {
            PlatformNameRules.requireFieldName(projection.targetField(), "projection.targetField");
            PlatformNameRules.requireFieldName(projection.outputField(), "projection.outputField");
            requireTargetField(config.getTargetMetadataId(), projection.targetField());
            requireAvailableOutputField(sourceMetadataId, projection.outputField(), "reference projection output field");
            if (!outputFields.add(projection.outputField())) {
                throw new PlatformException("Duplicate reference output field: " + projection.outputField());
            }
        }
    }

    private void requireTargetField(String targetMetadataId, String targetFieldName) {
        if (fieldService.count(Criteria.of()
                .eq("metadataId", targetMetadataId)
                .eq("fieldName", targetFieldName)) <= 0) {
            throw new PlatformException("Reference projection requires existing target field: " + targetFieldName);
        }
    }

    private void requireAvailableOutputField(String sourceMetadataId, String outputField, String name) {
        if (STANDARD_FIELDS.contains(outputField)
                || fieldService.count(Criteria.of()
                .eq("metadataId", sourceMetadataId)
                .eq("fieldName", outputField)) > 0) {
            throw new PlatformException(name + " conflicts with source field: " + outputField);
        }
    }

    private MetadataField requireField(String metadataFieldId, String name) {
        MetadataField field = metadataFieldId == null || metadataFieldId.isBlank() ? null : fieldService.select(metadataFieldId);
        if (field == null) {
            throw new PlatformException("Reference config requires existing " + name + ": " + metadataFieldId);
        }
        return field;
    }

    private String sourceFieldName(MetadataFieldReferenceConfig config) {
        return requireField(config.getMetadataFieldId(), "source metadata field").getFieldName();
    }
}
