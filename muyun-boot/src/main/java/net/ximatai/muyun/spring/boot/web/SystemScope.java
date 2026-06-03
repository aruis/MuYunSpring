package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.common.tenant.TenantContext;

import java.util.function.Supplier;

public interface SystemScope<S> extends ScopedWeb<S> {
    @Override
    default <T> T webScope(Supplier<T> action) {
        try (TenantContext.Scope ignored = TenantContext.system()) {
            return action.get();
        }
    }
}
