package net.ximatai.muyun.spring.ability.child;

public final class StaticChildResolverTestAccess {
    private StaticChildResolverTestAccess() {
    }

    public static void clearCacheForTests() {
        StaticChildResolver.clearCacheForTests();
    }
}
