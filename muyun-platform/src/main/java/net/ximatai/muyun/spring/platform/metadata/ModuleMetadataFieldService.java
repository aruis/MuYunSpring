package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMeasureUnitConversionMode;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMeasureUnitMode;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class ModuleMetadataFieldService extends AbstractAbilityService<ModuleMetadataField> implements
        SoftDeleteAbility<ModuleMetadataField>,
        SortAbility<ModuleMetadataField> {
    public static final String MODULE_ALIAS = "platform.module_metadata_field";
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final ModuleMetadataRelationService relationService;
    private final MetadataService metadataService;
    private final MetadataFieldService fieldService;
    private final PlatformFieldTypeService fieldTypeService;
    private final ModuleMetadataFieldReferenceGenerateRuleValidator referenceGenerateRuleValidator;

    public ModuleMetadataFieldService(BaseDao<ModuleMetadataField, String> moduleMetadataFieldDao,
                                      ModuleMetadataRelationService relationService,
                                      MetadataService metadataService,
                                      MetadataFieldService fieldService) {
        this(moduleMetadataFieldDao, relationService, metadataService, fieldService, null, Optional.empty());
    }

    public ModuleMetadataFieldService(BaseDao<ModuleMetadataField, String> moduleMetadataFieldDao,
                                      ModuleMetadataRelationService relationService,
                                      MetadataService metadataService,
                                      MetadataFieldService fieldService,
                                      Optional<ModuleMetadataFieldReferenceGenerateRuleValidator> referenceGenerateRuleValidator) {
        this(moduleMetadataFieldDao, relationService, metadataService, fieldService, null, referenceGenerateRuleValidator);
    }

    @Autowired
    public ModuleMetadataFieldService(BaseDao<ModuleMetadataField, String> moduleMetadataFieldDao,
                                      ModuleMetadataRelationService relationService,
                                      MetadataService metadataService,
                                      MetadataFieldService fieldService,
                                      PlatformFieldTypeService fieldTypeService,
                                      Optional<ModuleMetadataFieldReferenceGenerateRuleValidator> referenceGenerateRuleValidator) {
        super(MODULE_ALIAS, ModuleMetadataField.class, moduleMetadataFieldDao);
        this.relationService = relationService;
        this.metadataService = metadataService;
        this.fieldService = fieldService;
        this.fieldTypeService = fieldTypeService;
        this.referenceGenerateRuleValidator = referenceGenerateRuleValidator == null
                ? null
                : referenceGenerateRuleValidator.orElse(null);
    }

    @Override
    public void beforeInsert(ModuleMetadataField moduleField) {
        normalizeAndValidate(moduleField);
    }

    @Override
    public void beforeUpdate(ModuleMetadataField moduleField) {
        normalizeAndValidate(moduleField);
    }

    @Override
    public Criteria sortScope(ModuleMetadataField moduleField) {
        return Criteria.of().eq("relationId", moduleField.getRelationId());
    }

    @Override
    public void validateSortScope(ModuleMetadataField left, ModuleMetadataField right) {
        if (!Objects.equals(left.getRelationId(), right.getRelationId())) {
            throw new PlatformException("Module metadata field sort can only move records within the same relation");
        }
    }

    public List<ModuleMetadataField> ensureForRelation(String relationId) {
        ModuleMetadataRelation relation = requireRelation(relationId);
        List<MetadataField> fields = fieldService.list(
                Criteria.of().eq("metadataId", relation.getMetadataId()),
                ALL,
                Sort.asc(PlatformAbilityFields.SORT_FIELD)
        );
        for (MetadataField field : fields) {
            if (findByRelationAndField(relation.getId(), field.getId()) == null) {
                ModuleMetadataField moduleField = new ModuleMetadataField();
                moduleField.setRelationId(relation.getId());
                moduleField.setMetadataFieldId(field.getId());
                moduleField.setTitle(field.getTitle());
                moduleField.setSortOrder(field.getSortOrder());
                insert(moduleField);
            }
        }
        return listByRelationId(relation.getId());
    }

    public List<ModuleMetadataField> listByRelationId(String relationId) {
        if (relationId == null || relationId.isBlank()) {
            return List.of();
        }
        return list(Criteria.of().eq("relationId", relationId), ALL, Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    public List<ModuleMetadataField> listByModuleAlias(String moduleAlias) {
        String validAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        List<String> relationIds = relationService.list(Criteria.of().eq("moduleAlias", validAlias),
                        ALL, Sort.asc(PlatformAbilityFields.SORT_FIELD))
                .stream()
                .map(ModuleMetadataRelation::getId)
                .toList();
        if (relationIds.isEmpty()) {
            return List.of();
        }
        return list(Criteria.of().in("relationId", relationIds), ALL, Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    public List<ModuleMetadataField> listMainByModuleAlias(String moduleAlias) {
        String validAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        List<String> relationIds = relationService.list(Criteria.of()
                                .eq("moduleAlias", validAlias)
                                .eq("relationRole", RelationRole.MAIN),
                        ALL, Sort.asc(PlatformAbilityFields.SORT_FIELD))
                .stream()
                .map(ModuleMetadataRelation::getId)
                .toList();
        if (relationIds.isEmpty()) {
            return List.of();
        }
        return list(Criteria.of().in("relationId", relationIds), ALL, Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    public ResolvedModuleMetadataField resolve(String moduleMetadataFieldId) {
        ModuleMetadataField moduleField = moduleMetadataFieldId == null || moduleMetadataFieldId.isBlank()
                ? null
                : select(moduleMetadataFieldId);
        if (moduleField == null) {
            throw new PlatformException("Module metadata field requires existing config: " + moduleMetadataFieldId);
        }
        ModuleMetadataRelation relation = requireRelation(moduleField.getRelationId());
        Metadata metadata = requireMetadata(relation.getMetadataId());
        MetadataField field = requireField(moduleField.getMetadataFieldId());
        if (!metadata.getId().equals(field.getMetadataId())) {
            throw new PlatformException("Module metadata field metadata mismatch: " + moduleField.getId());
        }
        return new ResolvedModuleMetadataField(
                moduleField.getId(),
                relation.getModuleAlias(),
                relation.getId(),
                relation.getRelationAlias(),
                relation.getRelationRole(),
                metadata.getId(),
                metadata.getAlias(),
                metadata.getTitle(),
                field.getId(),
                field.getFieldName(),
                field.getColumnName(),
                field.getTitle(),
                field.getFieldTypeAlias()
        );
    }

    private void normalizeAndValidate(ModuleMetadataField moduleField) {
        ModuleMetadataRelation relation = requireRelation(moduleField.getRelationId());
        Metadata metadata = requireMetadata(relation.getMetadataId());
        MetadataField field = requireField(moduleField.getMetadataFieldId());
        if (!relation.getMetadataId().equals(field.getMetadataId())) {
            throw new PlatformException("Module metadata field requires field in relation metadata: "
                    + moduleField.getMetadataFieldId());
        }
        normalizeReferenceConfig(moduleField, metadata, relation);
        normalizeMeasureUnitConfig(moduleField, metadata, field);
        rejectDuplicate(moduleField, Criteria.of()
                        .eq("relationId", relation.getId())
                        .eq("metadataFieldId", field.getId()),
                "module metadata field must be unique: " + relation.getId() + "." + field.getId());
        moduleField.setRelationId(relation.getId());
        moduleField.setMetadataFieldId(field.getId());
    }

    private void normalizeReferenceConfig(ModuleMetadataField moduleField,
                                          Metadata metadata,
                                          ModuleMetadataRelation relation) {
        if (moduleField.getCloneable() == null) {
            moduleField.setCloneable(Boolean.FALSE);
        }
        boolean hasDictionaryApplication = hasText(moduleField.getDictionaryApplicationAlias());
        boolean hasDictionaryCategory = hasText(moduleField.getDictionaryCategoryAlias());
        if (!hasDictionaryApplication && !hasDictionaryCategory) {
            moduleField.setDictionaryApplicationAlias(null);
            moduleField.setDictionaryCategoryAlias(null);
        } else {
            if (!hasDictionaryCategory) {
                throw new PlatformException("dictionaryCategoryAlias must not be blank");
            }
            String applicationAlias = hasDictionaryApplication
                    ? PlatformNameRules.requireApplicationAlias(moduleField.getDictionaryApplicationAlias())
                    : metadata.getApplicationAlias();
            moduleField.setDictionaryApplicationAlias(applicationAlias);
            moduleField.setDictionaryCategoryAlias(PlatformNameRules.requireIdentifier(
                    moduleField.getDictionaryCategoryAlias(), "dictionaryCategoryAlias"));
        }
        boolean hasReferenceModule = hasText(moduleField.getReferenceModuleAlias());
        if (hasReferenceModule) {
            moduleField.setReferenceModuleAlias(PlatformNameRules.requireModuleAlias(moduleField.getReferenceModuleAlias()));
            moduleField.setReferenceModuleKeyField(PlatformNameRules.requireFieldName(
                    moduleField.getReferenceModuleKeyField(), "referenceModuleKeyField"));
            moduleField.setReferenceModuleLabelField(PlatformNameRules.requireFieldName(
                    moduleField.getReferenceModuleLabelField(), "referenceModuleLabelField"));
            if (hasText(moduleField.getReferenceGenerateRuleId()) && referenceGenerateRuleValidator == null) {
                throw new PlatformException("referenceGenerateRuleId requires generate rule validator");
            }
            if (hasText(moduleField.getReferenceGenerateRuleId())) {
                referenceGenerateRuleValidator.validateReferenceGenerateRule(
                        moduleField.getReferenceGenerateRuleId(),
                        moduleField.getReferenceModuleAlias(),
                        relation.getModuleAlias());
            }
        } else if (hasReferenceDependentConfig(moduleField)) {
            throw new PlatformException("reference module config requires referenceModuleAlias");
        }
        moduleField.setReferenceModulePlusFields(normalizeFieldNameSet(
                moduleField.getReferenceModulePlusFields(), "referenceModulePlusFields"));
    }

    private void normalizeMeasureUnitConfig(ModuleMetadataField moduleField,
                                            Metadata metadata,
                                            MetadataField field) {
        if (!hasText(moduleField.getUnitCategoryAlias())) {
            clearMeasureUnitConfig(moduleField);
            return;
        }
        if (field.getFieldRole() == MetadataFieldRole.MEASURE_UNIT
                || field.getFieldRole() == MetadataFieldRole.MEASURE_BASE_VALUE) {
            throw new PlatformException("measure unit config must be declared on owner value field: "
                    + field.getFieldName());
        }
        requireNumericField(field, "measure unit value field");
        moduleField.setUnitCategoryAlias(PlatformNameRules.requireIdentifier(
                moduleField.getUnitCategoryAlias(), "unitCategoryAlias"));
        moduleField.setBaseUnitCode(PlatformNameRules.requireIdentifier(moduleField.getBaseUnitCode(), "baseUnitCode"));
        if (moduleField.getUnitConversionMode() == null) {
            moduleField.setUnitConversionMode(FieldMeasureUnitConversionMode.LINEAR);
        }
        if (moduleField.getUnitMode() == null) {
            moduleField.setUnitMode(hasText(moduleField.getFixedUnitCode())
                    ? FieldMeasureUnitMode.FIXED
                    : FieldMeasureUnitMode.SELECTABLE);
        }
        if (moduleField.getUnitRequired() == null) {
            moduleField.setUnitRequired(Boolean.FALSE);
        }
        if (hasText(moduleField.getDefaultUnitCode())) {
            moduleField.setDefaultUnitCode(PlatformNameRules.requireIdentifier(
                    moduleField.getDefaultUnitCode(), "defaultUnitCode"));
        }
        if (moduleField.getUnitMode() == FieldMeasureUnitMode.FIXED) {
            moduleField.setFixedUnitCode(PlatformNameRules.requireIdentifier(moduleField.getFixedUnitCode(), "fixedUnitCode"));
            moduleField.setUnitFieldId(null);
            if (!hasText(moduleField.getDefaultUnitCode())) {
                moduleField.setDefaultUnitCode(moduleField.getFixedUnitCode());
            }
        } else {
            moduleField.setFixedUnitCode(null);
            MetadataField unitField = requireRelatedField(moduleField.getUnitFieldId(), metadata, field,
                    MetadataFieldForm.COMPANION, MetadataFieldRole.MEASURE_UNIT, "unitFieldId");
            requireFieldType(unitField, FieldType.STRING, "measure unit companion field");
            moduleField.setUnitFieldId(unitField.getId());
        }
        MetadataField baseValueField = requireRelatedField(moduleField.getBaseValueFieldId(), metadata, field,
                MetadataFieldForm.SHADOW, MetadataFieldRole.MEASURE_BASE_VALUE, "baseValueFieldId");
        requireNumericField(baseValueField, "measure base value shadow field");
        moduleField.setBaseValueFieldId(baseValueField.getId());
        if (hasText(moduleField.getConversionScopeFieldId())) {
            MetadataField scopeField = requireField(moduleField.getConversionScopeFieldId());
            if (!Objects.equals(scopeField.getMetadataId(), metadata.getId())) {
                throw new PlatformException("conversionScopeFieldId must belong to same metadata: "
                        + moduleField.getConversionScopeFieldId());
            }
            moduleField.setConversionScopeFieldId(scopeField.getId());
        }
    }

    private void clearMeasureUnitConfig(ModuleMetadataField moduleField) {
        moduleField.setUnitCategoryAlias(null);
        moduleField.setUnitMode(null);
        moduleField.setFixedUnitCode(null);
        moduleField.setDefaultUnitCode(null);
        moduleField.setUnitFieldId(null);
        moduleField.setBaseValueFieldId(null);
        moduleField.setBaseUnitCode(null);
        moduleField.setUnitConversionMode(null);
        moduleField.setConversionScopeFieldId(null);
        moduleField.setUnitRequired(Boolean.FALSE);
    }

    private MetadataField requireRelatedField(String fieldId,
                                              Metadata metadata,
                                              MetadataField owner,
                                              MetadataFieldForm form,
                                              MetadataFieldRole role,
                                              String label) {
        if (!hasText(fieldId)) {
            throw new PlatformException(label + " must not be blank");
        }
        MetadataField related = requireField(fieldId);
        if (!Objects.equals(related.getMetadataId(), metadata.getId())) {
            throw new PlatformException(label + " must belong to same metadata: " + fieldId);
        }
        if (related.getFieldForm() != form || related.getFieldRole() != role) {
            throw new PlatformException(label + " requires " + form + " " + role + " field: " + fieldId);
        }
        if (!Objects.equals(related.getOwnerFieldId(), owner.getId())) {
            throw new PlatformException(label + " must be owned by measure value field: " + fieldId);
        }
        return related;
    }

    private void requireNumericField(MetadataField field, String label) {
        FieldType type = requireFieldType(field);
        if (type != FieldType.INTEGER && type != FieldType.LONG && type != FieldType.DECIMAL) {
            throw new PlatformException(label + " requires numeric field: " + field.getFieldName());
        }
    }

    private void requireFieldType(MetadataField field, FieldType expected, String label) {
        FieldType type = requireFieldType(field);
        if (type != expected) {
            throw new PlatformException(label + " requires " + expected + " field: " + field.getFieldName());
        }
    }

    private FieldType requireFieldType(MetadataField field) {
        if (fieldTypeService == null) {
            throw new PlatformException("measure unit config requires PlatformFieldTypeService");
        }
        return fieldTypeService.requireFieldType(field.getFieldTypeAlias()).getFieldType();
    }

    private boolean hasReferenceDependentConfig(ModuleMetadataField moduleField) {
        return hasText(moduleField.getReferenceModuleKeyField())
                || hasText(moduleField.getReferenceModuleLabelField())
                || hasText(moduleField.getReferenceGenerateRuleId())
                || hasText(moduleField.getReferenceQueryTemplateId())
                || (moduleField.getReferenceModulePlusFields() != null
                && !moduleField.getReferenceModulePlusFields().isEmpty());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Set<String> normalizeFieldNameSet(Set<String> fields, String label) {
        if (fields == null || fields.isEmpty()) {
            return fields;
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String field : fields) {
            normalized.add(PlatformNameRules.requireFieldName(field, label));
        }
        return normalized;
    }

    private ModuleMetadataField findByRelationAndField(String relationId, String fieldId) {
        return findOne(Criteria.of()
                .eq("relationId", relationId)
                .eq("metadataFieldId", fieldId));
    }

    private ModuleMetadataRelation requireRelation(String relationId) {
        ModuleMetadataRelation relation = relationId == null || relationId.isBlank() ? null : relationService.select(relationId);
        if (relation == null) {
            throw new PlatformException("Module metadata field requires existing relation: " + relationId);
        }
        return relation;
    }

    private Metadata requireMetadata(String metadataId) {
        Metadata metadata = metadataId == null || metadataId.isBlank() ? null : metadataService.select(metadataId);
        if (metadata == null) {
            throw new PlatformException("Module metadata field requires existing metadata: " + metadataId);
        }
        return metadata;
    }

    private MetadataField requireField(String fieldId) {
        MetadataField field = fieldId == null || fieldId.isBlank() ? null : fieldService.select(fieldId);
        if (field == null) {
            throw new PlatformException("Module metadata field requires existing field: " + fieldId);
        }
        return field;
    }
}
