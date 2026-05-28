package net.ximatai.muyun.spring.common.model.contract;

/**
 * Baseline contract for platform-managed records.
 */
public interface EntityContract extends Identifiable, TenantScoped, Versioned, SoftDeletable, Auditable {
}
