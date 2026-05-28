package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.spring.common.model.EntityContract;

import java.time.Instant;

abstract class DynamicRecordView implements EntityContract {
    private final DynamicRecord record;

    DynamicRecordView(DynamicRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("record must not be null");
        }
        this.record = record;
    }

    DynamicRecord record() {
        return record;
    }

    @Override
    public String getId() {
        return record.getId();
    }

    @Override
    public void setId(String id) {
        record.setId(id);
    }

    @Override
    public String getTenantId() {
        return record.getTenantId();
    }

    @Override
    public void setTenantId(String tenantId) {
        record.setTenantId(tenantId);
    }

    @Override
    public Integer getVersion() {
        return record.getVersion();
    }

    @Override
    public void setVersion(Integer version) {
        record.setVersion(version);
    }

    @Override
    public Boolean getDeleted() {
        return record.getDeleted();
    }

    @Override
    public void setDeleted(Boolean deleted) {
        record.setDeleted(deleted);
    }

    @Override
    public Instant getDeletedAt() {
        return record.getDeletedAt();
    }

    @Override
    public void setDeletedAt(Instant deletedAt) {
        record.setDeletedAt(deletedAt);
    }

    @Override
    public String getCreatedBy() {
        return record.getCreatedBy();
    }

    @Override
    public void setCreatedBy(String createdBy) {
        record.setCreatedBy(createdBy);
    }

    @Override
    public Instant getCreatedAt() {
        return record.getCreatedAt();
    }

    @Override
    public void setCreatedAt(Instant createdAt) {
        record.setCreatedAt(createdAt);
    }

    @Override
    public String getUpdatedBy() {
        return record.getUpdatedBy();
    }

    @Override
    public void setUpdatedBy(String updatedBy) {
        record.setUpdatedBy(updatedBy);
    }

    @Override
    public Instant getUpdatedAt() {
        return record.getUpdatedAt();
    }

    @Override
    public void setUpdatedAt(Instant updatedAt) {
        record.setUpdatedAt(updatedAt);
    }
}
