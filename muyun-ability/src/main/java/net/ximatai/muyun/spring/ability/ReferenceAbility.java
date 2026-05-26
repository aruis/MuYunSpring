package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.model.BaseModel;
import net.ximatai.muyun.spring.common.model.TitledModel;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface ReferenceAbility<T extends BaseModel & TitledModel> extends CrudAbility<T> {
    default String title(String id) {
        T entity = select(id);
        return entity == null ? null : entity.getTitle();
    }

    default Map<String, String> titles(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<T> entities = getDao().query(
                Criteria.of().in("id", List.copyOf(ids)),
                new PageRequest(0, Integer.MAX_VALUE)
        );
        Map<String, String> titles = new LinkedHashMap<>();
        for (T entity : entities) {
            if (!Boolean.TRUE.equals(entity.getDeleted())) {
                titles.put(entity.getId(), entity.getTitle());
            }
        }
        return titles;
    }
}
