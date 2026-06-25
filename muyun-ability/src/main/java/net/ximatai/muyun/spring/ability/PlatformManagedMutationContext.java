package net.ximatai.muyun.spring.ability;

import java.util.function.Supplier;

/**
 * Marks mutations that intentionally maintain platform-managed records.
 */
public final class PlatformManagedMutationContext {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private PlatformManagedMutationContext() {
    }

    public static <T> T runAsPlatformManaged(Supplier<T> supplier) {
        int previous = DEPTH.get();
        DEPTH.set(previous + 1);
        try {
            return supplier.get();
        } finally {
            if (previous == 0) {
                DEPTH.remove();
            } else {
                DEPTH.set(previous);
            }
        }
    }

    public static void runAsPlatformManaged(Runnable runnable) {
        runAsPlatformManaged(() -> {
            runnable.run();
            return null;
        });
    }

    static boolean isPlatformManagedMutation() {
        return DEPTH.get() > 0;
    }
}
