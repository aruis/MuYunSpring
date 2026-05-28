package net.ximatai.muyun.spring.common.model.contract;

import java.time.Instant;

/**
 * Marks records that use logical deletion instead of physical deletion by default.
 */
public interface SoftDeletable {
    Boolean getDeleted();

    void setDeleted(Boolean deleted);

    Instant getDeletedAt();

    void setDeletedAt(Instant deletedAt);
}
