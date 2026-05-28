package net.ximatai.muyun.spring.ability.reference;

import java.util.Set;

public final class ReferenceDependencyRegistryTestAccess {
    private ReferenceDependencyRegistryTestAccess() {
    }

    public static Set<String> referrerIds(ReferenceTarget target, String id) {
        return ReferenceDependencyRegistry.referrerIds(target, id);
    }

    public static void clearAll() {
        ReferenceDependencyRegistry.clearAll();
    }
}
