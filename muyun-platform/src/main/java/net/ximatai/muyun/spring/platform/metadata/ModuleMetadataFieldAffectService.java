package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ModuleMetadataFieldAffectService extends AbstractAbilityService<ModuleMetadataFieldAffect> implements
        SoftDeleteAbility<ModuleMetadataFieldAffect>,
        SortAbility<ModuleMetadataFieldAffect> {
    public static final String MODULE_ALIAS = "platform.module_metadata_field_affect";

    private final ModuleMetadataFieldService moduleFieldService;

    public ModuleMetadataFieldAffectService(BaseDao<ModuleMetadataFieldAffect, String> affectDao,
                                            ModuleMetadataFieldService moduleFieldService) {
        super(MODULE_ALIAS, ModuleMetadataFieldAffect.class, affectDao);
        this.moduleFieldService = moduleFieldService;
    }

    @Override
    public void beforeInsert(ModuleMetadataFieldAffect affect) {
        normalizeAndValidate(affect);
    }

    @Override
    public void beforeUpdate(ModuleMetadataFieldAffect affect) {
        normalizeAndValidate(affect);
    }

    @Override
    public Criteria sortScope(ModuleMetadataFieldAffect affect) {
        return Criteria.of().eq("moduleMetadataFieldId", affect.getModuleMetadataFieldId());
    }

    @Override
    public void validateSortScope(ModuleMetadataFieldAffect left, ModuleMetadataFieldAffect right) {
        if (!Objects.equals(left.getModuleMetadataFieldId(), right.getModuleMetadataFieldId())) {
            throw new PlatformException("Module metadata field affect sort can only move records within the same field");
        }
    }

    private void normalizeAndValidate(ModuleMetadataFieldAffect affect) {
        ModuleMetadataField owner = requireModuleField(affect.getModuleMetadataFieldId(), "moduleMetadataFieldId");
        ModuleMetadataField referenceField = requireModuleField(affect.getReferenceFieldId(), "referenceFieldId");
        ModuleMetadataField targetField = requireModuleField(affect.getTargetFieldId(), "targetFieldId");
        if (!Objects.equals(owner.getRelationId(), targetField.getRelationId())) {
            throw new PlatformException("reference affect target field must belong to owner relation");
        }
        validateReferenceField(owner, referenceField);
        affect.setModuleMetadataFieldId(owner.getId());
        affect.setReferenceFieldId(referenceField.getId());
        affect.setTargetFieldId(targetField.getId());
    }

    private void validateReferenceField(ModuleMetadataField owner, ModuleMetadataField referenceField) {
        if (owner.getReferenceModuleAlias() == null || owner.getReferenceModuleAlias().isBlank()) {
            throw new PlatformException("reference affect requires owner referenceModuleAlias");
        }
        ResolvedModuleMetadataField resolved = moduleFieldService.resolve(referenceField.getId());
        if (!owner.getReferenceModuleAlias().equals(resolved.moduleAlias())) {
            throw new PlatformException("reference affect field must belong to reference module: "
                    + owner.getReferenceModuleAlias());
        }
        if (resolved.relationRole() != RelationRole.MAIN) {
            throw new PlatformException("reference affect field must belong to reference module main relation");
        }
    }

    private ModuleMetadataField requireModuleField(String moduleMetadataFieldId, String label) {
        ModuleMetadataField moduleField = moduleMetadataFieldId == null || moduleMetadataFieldId.isBlank()
                ? null
                : moduleFieldService.select(moduleMetadataFieldId);
        if (moduleField == null) {
            throw new PlatformException("Module metadata field affect requires existing " + label + ": "
                    + moduleMetadataFieldId);
        }
        return moduleField;
    }
}
