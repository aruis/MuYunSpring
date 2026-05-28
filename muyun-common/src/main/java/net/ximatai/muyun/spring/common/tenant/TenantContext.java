package net.ximatai.muyun.spring.common.tenant;

import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.Optional;

public final class TenantContext {
    private enum Mode {
        NONE,
        TENANT,
        SYSTEM
    }

    private record State(Mode mode, String tenantId) {
        private static State none() {
            return new State(Mode.NONE, null);
        }
    }

    private static final ThreadLocal<State> CURRENT = ThreadLocal.withInitial(State::none);

    private TenantContext() {
    }

    public static Optional<String> currentTenantId() {
        State state = CURRENT.get();
        return state.mode() == Mode.TENANT ? Optional.of(state.tenantId()) : Optional.empty();
    }

    public static boolean isSystem() {
        return CURRENT.get().mode() == Mode.SYSTEM;
    }

    public static boolean hasContext() {
        return CURRENT.get().mode() != Mode.NONE;
    }

    public static void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            CURRENT.set(State.none());
            return;
        }
        CURRENT.set(new State(Mode.TENANT, tenantId));
    }

    public static Scope use(String tenantId) {
        State previous = CURRENT.get();
        setTenantId(tenantId);
        return new Scope(previous);
    }

    public static Scope system() {
        State previous = CURRENT.get();
        CURRENT.set(new State(Mode.SYSTEM, null));
        return new Scope(previous);
    }

    public static void clear() {
        CURRENT.set(State.none());
    }

    public static void applyToNewEntity(EntityContract entity) {
        if (entity == null || (entity.getTenantId() != null && !entity.getTenantId().isBlank())) {
            return;
        }
        currentTenantId().ifPresent(entity::setTenantId);
    }

    public static boolean matchesCurrentTenant(EntityContract entity) {
        if (entity == null) {
            return false;
        }
        return currentTenantId()
                .map(tenantId -> tenantId.equals(entity.getTenantId()))
                .orElse(true);
    }

    public static final class Scope implements AutoCloseable {
        private final State previous;

        private Scope(State previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            CURRENT.set(previous);
        }
    }
}
