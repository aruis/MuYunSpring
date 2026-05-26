package net.ximatai.muyun.spring.common.model;

import java.time.Instant;

public interface EntityContract {
    String getId();

    void setId(String id);

    String getTenantId();

    void setTenantId(String tenantId);

    Integer getVersion();

    void setVersion(Integer version);

    Boolean getDeleted();

    void setDeleted(Boolean deleted);

    String getCreatedBy();

    void setCreatedBy(String createdBy);

    Instant getCreatedAt();

    void setCreatedAt(Instant createdAt);

    String getUpdatedBy();

    void setUpdatedBy(String updatedBy);

    Instant getUpdatedAt();

    void setUpdatedAt(Instant updatedAt);
}
