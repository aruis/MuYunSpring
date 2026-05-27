package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.model.EntityContract;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CacheRegistry {
    private static final Map<String, Map<String, EntityContract>> ITEM_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<? extends EntityContract>> ALL_CACHE = new ConcurrentHashMap<>();

    private CacheRegistry() {
    }

    static Map<String, EntityContract> itemCache(String namespace) {
        return ITEM_CACHE.computeIfAbsent(namespace, key -> new ConcurrentHashMap<>());
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
        return (List<T>) ALL_CACHE.get(namespace);
    }

    static void putAllCache(String namespace, List<? extends EntityContract> records) {
        ALL_CACHE.put(namespace, records);
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

    public static int namespaceCount() {
        java.util.HashSet<String> namespaces = new java.util.HashSet<>(ITEM_CACHE.keySet());
        namespaces.addAll(ALL_CACHE.keySet());
        return namespaces.size();
    }

    private static boolean matchesPrefix(String namespace, String prefix) {
        return namespace.equals(prefix) || namespace.startsWith(prefix + "::");
    }
}
