package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.spring.common.model.EntityContract;
import net.ximatai.muyun.spring.common.model.TitleFieldResolver;
import net.ximatai.muyun.spring.common.model.TitledCapable;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface ReferenceAbility<T extends EntityContract & TitledCapable> extends CrudAbility<T> {
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

    default T selectReferenceRaw(String id) {
        return select(id);
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
        if (title == null) {
            throw new AbilityException("reference entity requires @TitleField: " + entity.getClass().getName());
        }
        return title;
    }
}
