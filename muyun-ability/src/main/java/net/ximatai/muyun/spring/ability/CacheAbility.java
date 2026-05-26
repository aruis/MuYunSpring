package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.model.EntityContract;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface CacheAbility<T extends EntityContract> extends CrudAbility<T> {
    String ALL_CACHE_KEY = "__all__";

    class CacheHolder {
        private static final Map<String, Map<String, EntityContract>> ITEM_CACHE = new ConcurrentHashMap<>();
        private static final Map<String, List<? extends EntityContract>> ALL_CACHE = new ConcurrentHashMap<>();

        private CacheHolder() {
        }
    }

    default String cacheNamespace() {
        return getClass().getName()
                + "::" + getModuleAlias()
                + "::" + System.identityHashCode(getDao());
    }

    T copyForCache(T entity);

    @SuppressWarnings("unchecked")
    default T selectWithCache(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        Map<String, EntityContract> itemCache = CacheHolder.ITEM_CACHE.computeIfAbsent(cacheNamespace(), key -> new ConcurrentHashMap<>());
        T cached = (T) itemCache.get(id);
        if (cached != null) {
            if (!isCacheVisible(cached)) {
                itemCache.remove(id);
                return null;
            }
            T copied = copyForCache(cached);
            afterSelect(copied);
            return copied;
        }

        T loaded = getDao().findById(id);
        if (!isCacheVisible(loaded)) {
            return null;
        }
        itemCache.put(id, copyForCache(loaded));
        T copied = copyForCache(loaded);
        afterSelect(copied);
        return copied;
    }

    @SuppressWarnings("unchecked")
    default List<T> selectAllWithCache() {
        List<T> cached = (List<T>) CacheHolder.ALL_CACHE.get(cacheNamespace());
        if (cached == null) {
            cached = getDao().query(activeCriteria(Criteria.of()), new PageRequest(0, Integer.MAX_VALUE)).stream()
                    .map(this::copyForCache)
                    .toList();
            CacheHolder.ALL_CACHE.put(cacheNamespace(), cached);
        }
        return cached.stream()
                .map(this::copyForCache)
                .peek(this::afterSelect)
                .toList();
    }

    default void clearItemCache(String id) {
        Map<String, EntityContract> itemCache = CacheHolder.ITEM_CACHE.get(cacheNamespace());
        if (itemCache != null) {
            itemCache.remove(id);
        }
        clearAllCache();
    }

    default void clearAllCache() {
        CacheHolder.ALL_CACHE.remove(cacheNamespace());
    }

    default void clearCache() {
        CacheHolder.ITEM_CACHE.remove(cacheNamespace());
        clearAllCache();
    }

    private boolean isCacheVisible(T entity) {
        if (entity == null) {
            return false;
        }
        if (this instanceof SoftDeleteAbility<?> && Boolean.TRUE.equals(entity.getDeleted())) {
            return false;
        }
        return true;
    }
}
