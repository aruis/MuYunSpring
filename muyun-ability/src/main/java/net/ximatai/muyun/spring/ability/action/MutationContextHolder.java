package net.ximatai.muyun.spring.ability.action;

import java.util.Optional;

public final class MutationContextHolder {
    private static final ThreadLocal<MutationContext> CURRENT = new ThreadLocal<>();

    private MutationContextHolder() {
    }

    public static Optional<MutationContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static Scope use(MutationContext context) {
        MutationContext previous = CURRENT.get();
        CURRENT.set(context);
        return new Scope(previous);
    }

    public static final class Scope implements AutoCloseable {
        private final MutationContext previous;

        private Scope(MutationContext previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null) {
                CURRENT.remove();
                return;
            }
            CURRENT.set(previous);
        }
    }
}
