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
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ModuleMetadataFieldService extends AbstractAbilityService<ModuleMetadataField> implements
        SoftDeleteAbility<ModuleMetadataField>,
        SortAbility<ModuleMetadataField> {
    public static final String MODULE_ALIAS = "platform.moduleMetadataField";
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final ModuleMetadataRelationService relationService;
    private final MetadataService metadataService;
    private final MetadataFieldService fieldService;

    public ModuleMetadataFieldService(BaseDao<ModuleMetadataField, String> moduleMetadataFieldDao,
                                      ModuleMetadataRelationService relationService,
                                      MetadataService metadataService,
                                      MetadataFieldService fieldService) {
        super(MODULE_ALIAS, ModuleMetadataField.class, moduleMetadataFieldDao);
        this.relationService = relationService;
        this.metadataService = metadataService;
        this.fieldService = fieldService;
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
        normalizeReferenceConfig(moduleField, metadata);
        rejectDuplicate(moduleField, Criteria.of()
                        .eq("relationId", relation.getId())
                        .eq("metadataFieldId", field.getId()),
                "module metadata field must be unique: " + relation.getId() + "." + field.getId());
        moduleField.setRelationId(relation.getId());
        moduleField.setMetadataFieldId(field.getId());
    }

    private void normalizeReferenceConfig(ModuleMetadataField moduleField, Metadata metadata) {
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
        } else if (hasReferenceDependentConfig(moduleField)) {
            throw new PlatformException("reference module config requires referenceModuleAlias");
        }
        moduleField.setReferenceModulePlusFields(normalizeFieldNameSet(
                moduleField.getReferenceModulePlusFields(), "referenceModulePlusFields"));
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
