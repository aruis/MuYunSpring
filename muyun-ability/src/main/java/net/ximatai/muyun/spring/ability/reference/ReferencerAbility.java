package net.ximatai.muyun.spring.ability.reference;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public interface ReferencerAbility<T extends EntityContract> extends CrudAbility<T> {
    default List<ReferenceLookup> referenceLookups() {
        return List.of();
    }

    default ReferenceLookup referenceLookup(ReferenceAbility<?> ability) {
        return ReferenceLookup.of(ability);
    }

    default Map<ReferenceTarget, Set<String>> collectReferenceIdsByTarget(T entity) {
        Class<?> modelClass = referenceModelClass(entity);
        return modelClass == null
                ? Map.of()
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
        Class<?> modelClass = referenceModelClass(entity);
        if (modelClass == null) {
            return;
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
        return requireReferenceLookup(target, "title").titles(ids);
    }

    default Map<String, Map<String, Object>> referenceProjections(ReferenceTarget target,
                                                                  Collection<String> ids,
                                                                  Collection<String> sourceFields) {
        return requireReferenceLookup(target, "projection").projections(ids, sourceFields);
    }

    private ReferenceLookup requireReferenceLookup(ReferenceTarget target, String purpose) {
        if (target == null) {
            throw new PlatformException("reference " + purpose + " resolver target must not be null");
        }
        Map<ReferenceTarget, ReferenceLookup> lookups = referenceLookupsByTarget();
        ReferenceLookup lookup = lookups.get(target);
        if (lookup != null) {
            return lookup;
        }
        throw new PlatformException("reference " + purpose + " resolver is not configured: "
                + target.qualifiedName()
                + ", configured targets: "
                + lookups.keySet().stream().map(ReferenceTarget::qualifiedName).toList());
    }

    private Map<ReferenceTarget, ReferenceLookup> referenceLookupsByTarget() {
        Map<ReferenceTarget, ReferenceLookup> lookups = new LinkedHashMap<>();
        for (ReferenceLookup lookup : referenceLookups()) {
            ReferenceLookup previous = lookups.putIfAbsent(lookup.target(), lookup);
            if (previous != null) {
                throw new PlatformException("duplicate reference lookup target: " + lookup.target().qualifiedName());
            }
        }
        return lookups;
    }

    private Class<?> referenceModelClass(T entity) {
        Class<?> modelClass = modelClass();
        if (modelClass != null) {
            return modelClass;
        }
        return entity == null ? null : entity.getClass();
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
