package net.ximatai.muyun.spring.common.tenant;

import net.ximatai.muyun.spring.common.model.EntityContract;

import java.util.Optional;

public final class TenantContext {
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Optional<String> currentTenantId() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    public static void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            CURRENT_TENANT.remove();
            return;
        }
        CURRENT_TENANT.set(tenantId);
    }

    public static Scope use(String tenantId) {
        String previous = CURRENT_TENANT.get();
        setTenantId(tenantId);
        return new Scope(previous);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
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
        private final String previousTenantId;

        private Scope(String previousTenantId) {
            this.previousTenantId = previousTenantId;
        }

        @Override
        public void close() {
            setTenantId(previousTenantId);
        }
    }
}
