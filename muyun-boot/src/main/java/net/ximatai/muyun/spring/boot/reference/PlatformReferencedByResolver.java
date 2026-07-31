package net.ximatai.muyun.spring.boot.reference;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.reference.ReferencedByResolver;
import net.ximatai.muyun.spring.ability.reference.StaticReferencedByResolver;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.List;

/** Bridges static {@code @ReferencedBy} declarations to their unique CRUD source services. */
public final class PlatformReferencedByResolver implements ReferencedByResolver {
    private final StaticAbilityCatalog abilities;

    public PlatformReferencedByResolver(StaticAbilityCatalog abilities) {
        this.abilities = abilities;
        validateDeclarations();
    }

    @Override
    public void populate(CrudAbility<?> ability, EntityContract entity) {
        if (ability == null || entity == null) {
            return;
        }
        Class<?> targetModel = ability.modelClass() == null ? entity.getClass() : ability.modelClass();
        for (StaticReferencedByResolver.ReferencedByPlan plan : StaticReferencedByResolver.plans(targetModel)) {
            CrudAbility<?> sourceAbility = requireSourceAbility(targetModel, plan.sourceModel());
            StaticReferencedByResolver.writeLoadedValue(entity, plan.fieldName(),
                    sourceRows(sourceAbility, plan.sourceField(), entity.getId()));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List<? extends EntityContract> sourceRows(CrudAbility<?> sourceAbility,
                                                               String sourceField,
                                                               String targetId) {
        if (targetId == null || targetId.isBlank()) {
            return List.of();
        }
        return ((CrudAbility) sourceAbility).list(Criteria.of().eq(sourceField, targetId));
    }

    private void validateDeclarations() {
        for (CrudAbility<?> ability : abilities.abilities()) {
            Class<?> targetModel = ability.modelClass();
            for (StaticReferencedByResolver.ReferencedByPlan plan : StaticReferencedByResolver.plans(targetModel)) {
                requireSourceAbility(targetModel, plan.sourceModel());
            }
        }
    }

    private CrudAbility<?> requireSourceAbility(Class<?> targetModel, Class<?> sourceModel) {
        return abilities.findByModel(sourceModel).orElseThrow(() -> new PlatformException(
                "@ReferencedBy source service is not registered: "
                        + targetModel.getName() + " <- " + sourceModel.getName()));
    }
}
