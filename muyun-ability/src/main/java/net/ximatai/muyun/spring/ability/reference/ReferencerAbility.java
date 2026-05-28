package net.ximatai.muyun.spring.ability.reference;

import net.ximatai.muyun.spring.ability.AbilityException;
import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public interface ReferencerAbility<T extends EntityContract> extends CrudAbility<T> {
    default Class<?> referencingModelClass() {
        return null;
    }

    default List<ReferenceLookup> referenceLookups() {
        return List.of();
    }

    default ReferenceLookup referenceLookup(ReferenceAbility<?> ability) {
        return ReferenceLookup.of(ability);
    }

    default Map<ReferenceTarget, Set<String>> collectReferenceIdsByTarget(T entity) {
        Class<?> modelClass = referencingModelClass();
        return modelClass == null
                ? StaticReferenceResolver.collect(entity)
                : StaticReferenceResolver.collect(modelClass, entity);
    }

    default void afterReferenceSelect(T entity) {
        populateStaticReferenceTitles(entity);
    }

    default void refreshReferenceDependencies(T entity) {
        ReferenceDependencyRegistry.refresh(this, entity);
    }

    default void clearReferenceDependency(String id) {
        if (this instanceof CacheAbility<?> cacheAbility) {
            ReferenceDependencyRegistry.removeReferrer(cacheAbility.cacheNamespace(), id);
        }
    }

    default void populateStaticReferenceTitles(T entity) {
        Class<?> modelClass = referencingModelClass();
        if (modelClass == null || entity == null) {
            if (entity == null) {
                return;
            }
            modelClass = entity.getClass();
        }
        for (ReferencePlan plan : StaticReferenceResolver.plans(modelClass)) {
            if (!plan.autoTitle() && plan.projections().isEmpty()) {
                continue;
            }
            List<String> ids = referenceSourceValues(entity, plan);
            if (ids.isEmpty()) {
                StaticReferenceResolver.writeTitleValue(entity, plan.titleOutputField(), null);
                clearProjectionValues(entity, plan);
                continue;
            }
            if (plan.autoTitle()) {
                Object titleValue = referenceTitleValue(ids, referenceTitles(plan.target(), ids), plan);
                StaticReferenceResolver.writeTitleValue(entity, plan.titleOutputField(), titleValue);
            }
            populateProjectionValues(entity, plan, ids);
        }
    }

    default List<String> referenceSourceValues(T entity, ReferencePlan plan) {
        return StaticReferenceResolver.values(entity, plan);
    }

    default Map<String, String> referenceTitles(ReferenceTarget target, Collection<String> ids) {
        ReferenceLookup lookup = referenceLookupFor(target);
        if (lookup != null) {
            return lookup.titles(ids);
        }
        throw new AbilityException("reference title resolver is not configured: " + target.qualifiedName());
    }

    default Map<String, Map<String, Object>> referenceProjections(ReferenceTarget target,
                                                                  Collection<String> ids,
                                                                  Collection<String> sourceFields) {
        ReferenceLookup lookup = referenceLookupFor(target);
        if (lookup != null) {
            return lookup.projections(ids, sourceFields);
        }
        throw new AbilityException("reference projection resolver is not configured: " + target.qualifiedName());
    }

    private ReferenceLookup referenceLookupFor(ReferenceTarget target) {
        if (target == null) {
            return null;
        }
        for (ReferenceLookup lookup : referenceLookups()) {
            if (target.equals(lookup.target())) {
                return lookup;
            }
        }
        return null;
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

    private void populateProjectionValues(T entity, ReferencePlan plan, List<String> ids) {
        if (plan.projections().isEmpty()) {
            return;
        }
        Map<String, Map<String, Object>> loaded = referenceProjections(plan.target(), ids, projectionSourceFields(plan));
        for (ReferenceProjection projection : plan.projections()) {
            StaticReferenceResolver.writeTitleValue(entity, projection.outputField(),
                    referenceProjectionValue(ids, loaded, plan, projection.targetField()));
        }
    }

    private void clearProjectionValues(T entity, ReferencePlan plan) {
        for (ReferenceProjection projection : plan.projections()) {
            StaticReferenceResolver.writeTitleValue(entity, projection.outputField(), null);
        }
    }

    private List<String> projectionSourceFields(ReferencePlan plan) {
        return plan.projections().stream()
                .map(ReferenceProjection::targetField)
                .distinct()
                .toList();
    }

    private Object referenceProjectionValue(List<String> ids,
                                            Map<String, Map<String, Object>> loaded,
                                            ReferencePlan plan,
                                            String sourceField) {
        if (loaded == null) {
            loaded = Map.of();
        }
        Map<String, Map<String, Object>> loadedValues = loaded;
        if (plan.cardinality() == ReferenceCardinality.MANY) {
            return ids.stream()
                    .map(id -> fieldValue(loadedValues, id, sourceField))
                    .filter(Objects::nonNull)
                    .toList();
        }
        return fieldValue(loadedValues, ids.getFirst(), sourceField);
    }

    private Object fieldValue(Map<String, Map<String, Object>> loaded, String id, String sourceField) {
        Map<String, Object> fields = loaded.get(id);
        return fields == null ? null : fields.get(sourceField);
    }
}
