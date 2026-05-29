package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.springframework.stereotype.Service;

@Service
public class ModuleMetadataRelationService extends AbstractAbilityService<ModuleMetadataRelation> implements
        SoftDeleteAbility<ModuleMetadataRelation>,
        SortAbility<ModuleMetadataRelation> {
    public static final String MODULE_ALIAS = "platform.moduleMetadataRelation";

    private final PlatformModuleService moduleService;
    private final MetadataService metadataService;

    public ModuleMetadataRelationService(BaseDao<ModuleMetadataRelation, String> relationDao,
                                         PlatformModuleService moduleService,
                                         MetadataService metadataService) {
        super(MODULE_ALIAS, ModuleMetadataRelation.class, relationDao);
        this.moduleService = moduleService;
        this.metadataService = metadataService;
    }

    @Override
    public void beforeInsert(ModuleMetadataRelation relation) {
        normalizeAndValidate(relation);
    }

    @Override
    public void beforeUpdate(ModuleMetadataRelation relation) {
        normalizeAndValidate(relation);
    }

    @Override
    public Criteria sortScope(ModuleMetadataRelation relation) {
        return Criteria.of().eq("moduleAlias", relation.getModuleAlias());
    }

    @Override
    public void validateSortScope(ModuleMetadataRelation left, ModuleMetadataRelation right) {
        if (!java.util.Objects.equals(left.getModuleAlias(), right.getModuleAlias())) {
            throw new PlatformException("Module metadata relation sort can only move records within the same module");
        }
    }

    private void normalizeAndValidate(ModuleMetadataRelation relation) {
        String moduleAlias = PlatformNameRules.requireModuleAlias(relation.getModuleAlias());
        PlatformModule module = moduleService.select(moduleAlias);
        if (module == null) {
            throw new PlatformException("Module metadata relation requires existing module: " + moduleAlias);
        }
        Metadata metadata = metadataService.select(relation.getMetadataId());
        if (metadata == null) {
            throw new PlatformException("Module metadata relation requires existing metadata: " + relation.getMetadataId());
        }
        if (relation.getRelationRole() == null) {
            relation.setRelationRole(RelationRole.MAIN);
        }
        if (relation.getRelationAlias() == null || relation.getRelationAlias().isBlank()) {
            relation.setRelationAlias(metadata.getAlias());
        }
        PlatformNameRules.requireIdentifier(relation.getRelationAlias(), "relationAlias");
        if (relation.getRelationRole() == RelationRole.MAIN) {
            rejectDuplicateMainRelation(relation);
        } else {
            validateChildOrRelatedRelation(relation);
        }
        rejectDuplicateRelationAlias(relation);
        if (relation.getAutoPopulate() == null) {
            relation.setAutoPopulate(Boolean.FALSE);
        }
        if (relation.getCascadeDelete() == null) {
            relation.setCascadeDelete(Boolean.FALSE);
        }
        relation.setModuleAlias(moduleAlias);
    }

    private void rejectDuplicateMainRelation(ModuleMetadataRelation relation) {
        rejectDuplicate(relation, Criteria.of()
                        .eq("moduleAlias", relation.getModuleAlias())
                        .eq("relationRole", RelationRole.MAIN),
                "Module can only have one MAIN metadata relation: " + relation.getModuleAlias());
        relation.setParentMetadataId(null);
        relation.setForeignKey(null);
    }

    private void validateChildOrRelatedRelation(ModuleMetadataRelation relation) {
        if (relation.getParentMetadataId() == null || relation.getParentMetadataId().isBlank()) {
            throw new PlatformException("Non-main relation requires parentMetadataId");
        }
        if (metadataService.select(relation.getParentMetadataId()) == null) {
            throw new PlatformException("Relation requires existing parent metadata: " + relation.getParentMetadataId());
        }
        if (count(Criteria.of()
                .eq("moduleAlias", relation.getModuleAlias())
                .eq("metadataId", relation.getParentMetadataId())) <= 0) {
            throw new PlatformException("Relation requires parent metadata relation in same module: "
                    + relation.getParentMetadataId());
        }
        if (relation.getForeignKey() == null || relation.getForeignKey().isBlank()) {
            throw new PlatformException("Non-main relation requires foreignKey");
        }
        PlatformNameRules.requireFieldName(relation.getForeignKey(), "foreignKey");
    }

    private void rejectDuplicateRelationAlias(ModuleMetadataRelation relation) {
        rejectDuplicate(relation, Criteria.of()
                        .eq("moduleAlias", relation.getModuleAlias())
                        .eq("relationAlias", relation.getRelationAlias()),
                "relationAlias must be unique within module: " + relation.getRelationAlias());
    }
}
