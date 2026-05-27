package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.model.EntityContract;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CacheRegistry {
    private static final CachePolicy DEFAULT_POLICY = new CachePolicy(1024, Duration.ofMinutes(10));
    private static final Map<String, Map<String, EntityContract>> ITEM_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, AllCacheEntry> ALL_CACHE = new ConcurrentHashMap<>();

    private static volatile CachePolicy policy = DEFAULT_POLICY;

    private CacheRegistry() {
    }

    private static Map<String, EntityContract> itemCache(String namespace) {
        return ITEM_CACHE.computeIfAbsent(namespace, key -> boundedItemCache());
    }

    static EntityContract item(String namespace, String id) {
        Map<String, EntityContract> itemCache = ITEM_CACHE.get(namespace);
        return itemCache == null ? null : itemCache.get(id);
    }

    static void putItem(String namespace, String id, EntityContract entity) {
        itemCache(namespace).put(id, entity);
    }

    static void removeItem(String namespace, String id) {
        Map<String, EntityContract> itemCache = ITEM_CACHE.get(namespace);
        if (itemCache != null) {
            itemCache.remove(id);
            if (itemCache.isEmpty()) {
                ITEM_CACHE.remove(namespace, itemCache);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends EntityContract> List<T> allCache(String namespace) {
        AllCacheEntry entry = ALL_CACHE.get(namespace);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired(policy.allCacheTtl())) {
            ALL_CACHE.remove(namespace, entry);
            return null;
        }
        return (List<T>) entry.records();
    }

    static void putAllCache(String namespace, List<? extends EntityContract> records) {
        ALL_CACHE.put(namespace, new AllCacheEntry(records, Instant.now()));
    }

    public static void clearNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return;
        }
        ITEM_CACHE.remove(namespace);
        ALL_CACHE.remove(namespace);
    }

    static void clearAllCache(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return;
        }
        ALL_CACHE.remove(namespace);
    }

    public static void clearNamespacePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return;
        }
        ITEM_CACHE.keySet().removeIf(namespace -> matchesPrefix(namespace, prefix));
        ALL_CACHE.keySet().removeIf(namespace -> matchesPrefix(namespace, prefix));
    }

    public static void clearAll() {
        ITEM_CACHE.clear();
        ALL_CACHE.clear();
    }

    public static void configure(CachePolicy nextPolicy) {
        policy = Objects.requireNonNull(nextPolicy, "nextPolicy must not be null");
        ITEM_CACHE.clear();
        ALL_CACHE.clear();
    }

    public static void resetPolicy() {
        configure(DEFAULT_POLICY);
    }

    public static int namespaceCount() {
        HashSet<String> namespaces = new HashSet<>(ITEM_CACHE.keySet());
        namespaces.addAll(ALL_CACHE.keySet());
        return namespaces.size();
    }

    static Set<String> itemIds(String namespace) {
        Map<String, EntityContract> itemCache = ITEM_CACHE.get(namespace);
        if (itemCache == null) {
            return Set.of();
        }
        synchronized (itemCache) {
            return Set.copyOf(itemCache.keySet());
        }
    }

    private static Map<String, EntityContract> boundedItemCache() {
        return Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, EntityContract> eldest) {
                return size() > policy.maxItemsPerNamespace();
            }
        });
    }

    private static boolean matchesPrefix(String namespace, String prefix) {
        return namespace.equals(prefix) || namespace.startsWith(prefix + "::");
    }

    private record AllCacheEntry(List<? extends EntityContract> records, Instant cachedAt) {
        private boolean isExpired(Duration ttl) {
            return !ttl.isZero() && !ttl.isNegative() && cachedAt.plus(ttl).isBefore(Instant.now());
        }
    }

    public record CachePolicy(int maxItemsPerNamespace, Duration allCacheTtl) {
        public CachePolicy {
            if (maxItemsPerNamespace < 1) {
                throw new IllegalArgumentException("maxItemsPerNamespace must be positive");
            }
            allCacheTtl = Objects.requireNonNull(allCacheTtl, "allCacheTtl must not be null");
        }
    }
}
