package net.ximatai.muyun.spring.ability.reference;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.model.title.TitleFieldResolver;
import net.ximatai.muyun.spring.common.model.capability.TitledCapable;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface ReferenceAbility<T extends EntityContract & TitledCapable> extends CrudAbility<T> {
    default ReferenceTarget referenceTarget() {
        String moduleAlias = getModuleAlias();
        int separatorIndex = moduleAlias.lastIndexOf('.');
        if (separatorIndex <= 0 || separatorIndex == moduleAlias.length() - 1) {
            throw new PlatformException("reference target requires '<moduleAlias>.<entityCode>': " + moduleAlias);
        }
        return ReferenceTarget.of(moduleAlias.substring(0, separatorIndex), moduleAlias.substring(separatorIndex + 1));
    }

    default void clearReferenceReferrers(String id) {
        ReferenceDependencyRegistry.clearReferrers(referenceTarget(), id);
    }

    default String title(String id) {
        T entity = selectReferenceRaw(id);
        return entity == null ? null : referenceTitle(entity);
    }

    default Map<String, String> titles(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        LinkedHashSet<String> normalizedIds = new LinkedHashSet<>(ids);
        List<T> entities = getDao().query(
                activeCriteria(Criteria.of().in(StandardEntitySchema.ID_FIELD, List.copyOf(normalizedIds))),
                new PageRequest(0, Integer.MAX_VALUE)
        );
        Map<String, String> loadedTitles = new LinkedHashMap<>();
        for (T entity : entities) {
            loadedTitles.put(entity.getId(), referenceTitle(entity));
        }
        Map<String, String> titles = new LinkedHashMap<>();
        for (String id : normalizedIds) {
            if (loadedTitles.containsKey(id)) {
                titles.put(id, loadedTitles.get(id));
            }
        }
        return titles;
    }

    default Map<String, Map<String, Object>> projections(Collection<String> ids, Collection<String> fieldNames) {
        if (ids == null || ids.isEmpty() || fieldNames == null || fieldNames.isEmpty()) {
            return Map.of();
        }
        LinkedHashSet<String> normalizedIds = new LinkedHashSet<>(ids);
        LinkedHashSet<String> normalizedFields = new LinkedHashSet<>(fieldNames);
        List<T> entities = getDao().query(
                activeCriteria(Criteria.of().in(StandardEntitySchema.ID_FIELD, List.copyOf(normalizedIds))),
                new PageRequest(0, Integer.MAX_VALUE)
        );
        Map<String, Map<String, Object>> loaded = new LinkedHashMap<>();
        for (T entity : entities) {
            Map<String, Object> values = new LinkedHashMap<>();
            for (String fieldName : normalizedFields) {
                values.put(fieldName, ReferenceFieldResolver.read(entity, fieldName));
            }
            loaded.put(entity.getId(), Collections.unmodifiableMap(new LinkedHashMap<>(values)));
        }
        Map<String, Map<String, Object>> ordered = new LinkedHashMap<>();
        for (String id : normalizedIds) {
            if (loaded.containsKey(id)) {
                ordered.put(id, loaded.get(id));
            }
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(ordered));
    }

    default T selectReferenceRaw(String id) {
        return selectActiveRaw(id);
    }

    default PageResult<ReferenceOption> referenceOptions(Criteria criteria, PageRequest pageRequest) {
        PageResult<T> page = pageQuery(criteria, pageRequest);
        return PageResult.of(
                page.getRecords().stream()
                        .map(entity -> new ReferenceOption(entity.getId(), referenceTitle(entity)))
                        .toList(),
                page.getTotal(),
                pageRequest
        );
    }

    default String referenceTitle(T entity) {
        String title = TitleFieldResolver.readAsString(entity);
        if (title != null) {
            return title;
        }
        title = entity.getTitle();
        if (title != null) {
            return title;
        }
        throw new PlatformException("reference entity requires @TitleField or non-null TitledCapable title: "
                + entity.getClass().getName());
    }
}
