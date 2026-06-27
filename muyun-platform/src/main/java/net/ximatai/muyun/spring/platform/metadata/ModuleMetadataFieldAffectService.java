package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.runtime.PlatformDynamicRuntimeRefreshCoordinator;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryField;
import net.ximatai.muyun.spring.ability.query.QueryOperator;
import net.ximatai.muyun.spring.ability.query.QueryValueType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class ModuleMetadataFieldAffectService extends AbstractAbilityService<ModuleMetadataFieldAffect> implements
        SoftDeleteAbility<ModuleMetadataFieldAffect>,
        SortAbility<ModuleMetadataFieldAffect>, QueryAbility<ModuleMetadataFieldAffect>
{
    public static final String MODULE_ALIAS = "platform.module_metadata_field_affect";

    private final ModuleMetadataFieldService moduleFieldService;
    private final PlatformDynamicRuntimeRefreshCoordinator runtimeRefreshCoordinator;

    public ModuleMetadataFieldAffectService(BaseDao<ModuleMetadataFieldAffect, String> affectDao,
                                            ModuleMetadataFieldService moduleFieldService) {
        this(affectDao, moduleFieldService, Optional.empty());
    }


    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptor.builder(MODULE_ALIAS)
                .field(QueryField.of("id", QueryOperator.EQ, QueryOperator.IN).withTitle("ID"))
                .field(QueryField.of("moduleMetadataFieldId", QueryOperator.EQ, QueryOperator.IN).withTitle("模块字段"))
                .field(QueryField.of("referenceFieldId", QueryOperator.EQ, QueryOperator.IN).withTitle("引用字段"))
                .field(QueryField.of("targetFieldId", QueryOperator.EQ, QueryOperator.IN).withTitle("目标字段"))
                .field(QueryField.of("sortOrder", QueryValueType.INTEGER, QueryOperator.EQ)
                .withTitle("排序号").withSortable())
                .field(QueryField.of("createdAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                        QueryOperator.BETWEEN)
                .withTitle("创建时间").withSortable())
                .field(QueryField.of("updatedAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                        QueryOperator.BETWEEN)
                .withTitle("更新时间").withSortable())
                .defaultSort(Sort.asc("sortOrder"))
                .build();
    }
    @Autowired
    public ModuleMetadataFieldAffectService(BaseDao<ModuleMetadataFieldAffect, String> affectDao,
                                            ModuleMetadataFieldService moduleFieldService,
                                            Optional<PlatformDynamicRuntimeRefreshCoordinator> runtimeRefreshCoordinator) {
        super(MODULE_ALIAS, ModuleMetadataFieldAffect.class, affectDao);
        this.moduleFieldService = moduleFieldService;
        this.runtimeRefreshCoordinator = runtimeRefreshCoordinator.orElse(null);
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

    @Override
    public void afterChanged(ModuleMetadataFieldAffect affect) {
        if (runtimeRefreshCoordinator != null) {
            runtimeRefreshCoordinator.refreshByFieldAffect(affect);
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
