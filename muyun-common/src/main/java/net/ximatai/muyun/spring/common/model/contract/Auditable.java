package net.ximatai.muyun.spring.common.model.contract;

import java.time.Instant;

/**
 * Marks records that carry creation and update audit metadata.
 */
public interface Auditable {
    String getCreatedBy();

    void setCreatedBy(String createdBy);

    Instant getCreatedAt();

    void setCreatedAt(Instant createdAt);

    String getUpdatedBy();

    void setUpdatedBy(String updatedBy);

    Instant getUpdatedAt();

    void setUpdatedAt(Instant updatedAt);
}
