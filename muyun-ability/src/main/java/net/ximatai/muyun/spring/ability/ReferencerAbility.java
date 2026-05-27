package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.model.EntityContract;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public interface ReferencerAbility<T extends EntityContract> extends CrudAbility<T> {
    default Class<?> referencingModelClass() {
        return null;
    }

    default Map<ReferenceTarget, Set<String>> collectReferenceIdsByTarget(T entity) {
        Class<?> modelClass = referencingModelClass();
        return modelClass == null ? Map.of() : StaticReferenceResolver.collect(modelClass, entity);
    }

    @Override
    default void afterPlatformSelect(T entity) {
        populateStaticReferenceTitles(entity);
    }

    default void populateStaticReferenceTitles(T entity) {
        Class<?> modelClass = referencingModelClass();
        if (modelClass == null || entity == null) {
            return;
        }
        for (ReferencePlan plan : StaticReferenceResolver.plans(modelClass)) {
            if (!plan.autoTitle()) {
                continue;
            }
            List<String> ids = referenceSourceValues(entity, plan);
            if (ids.isEmpty()) {
                StaticReferenceResolver.writeTitleValue(entity, plan.titleOutputField(), null);
                continue;
            }
            Object titleValue = referenceTitleValue(ids, referenceTitles(plan.target(), ids), plan);
            StaticReferenceResolver.writeTitleValue(entity, plan.titleOutputField(), titleValue);
        }
    }

    default List<String> referenceSourceValues(T entity, ReferencePlan plan) {
        return StaticReferenceResolver.values(entity, plan);
    }

    default Map<String, String> referenceTitles(ReferenceTarget target, Collection<String> ids) {
        throw new AbilityException("reference title resolver is not configured: " + target.qualifiedName());
    }

    private Object referenceTitleValue(List<String> ids, Map<String, String> titles, ReferencePlan plan) {
        if (ids.isEmpty()) {
            return null;
        }
        if (titles == null) {
            titles = Map.of();
        }
        if (plan.cardinality() == ReferenceCardinality.MANY) {
            return ids.stream()
                    .map(titles::get)
                    .filter(Objects::nonNull)
                    .toList();
        }
        return titles.get(ids.getFirst());
    }
}
