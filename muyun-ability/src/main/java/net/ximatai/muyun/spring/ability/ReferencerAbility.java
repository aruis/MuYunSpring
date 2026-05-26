package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.model.EntityContract;

import java.util.Map;
import java.util.Set;

public interface ReferencerAbility<T extends EntityContract> extends CrudAbility<T> {
    default Map<String, Set<String>> collectReferenceIdsBySourceNamespace(T entity) {
        return Map.of();
    }
}
