package net.ximatai.muyun.spring.dynamic.runtime;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class DynamicMutationContext implements AutoCloseable {
    private static final ThreadLocal<DynamicMutationContext> CURRENT = new ThreadLocal<>();

    private final DynamicMutationContext previous;
    private final Instant startedAt;
    private final boolean owner;

    private DynamicMutationContext(DynamicMutationContext previous, Instant startedAt, boolean owner) {
        this.previous = previous;
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        this.owner = owner;
    }

    public static DynamicMutationContext open(Clock clock) {
        DynamicMutationContext existing = CURRENT.get();
        if (existing != null) {
            return new DynamicMutationContext(existing, existing.startedAt, false);
        }
        Clock effectiveClock = clock == null ? Clock.systemDefaultZone() : clock;
        DynamicMutationContext context = new DynamicMutationContext(null, effectiveClock.instant(), true);
        CURRENT.set(context);
        return context;
    }

    public static Optional<DynamicMutationContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public Instant startedAt() {
        return startedAt;
    }

    @Override
    public void close() {
        if (owner) {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
