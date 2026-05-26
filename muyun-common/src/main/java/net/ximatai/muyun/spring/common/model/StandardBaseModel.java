package net.ximatai.muyun.spring.common.model;

import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Id;
import net.ximatai.muyun.database.core.builder.ColumnType;

import java.time.Instant;

public abstract class StandardBaseModel implements BaseModel {
    @Id
    @Column(name = "id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "ID")
    private String id;

    @Column(name = "version", type = ColumnType.INT, comment = "Optimistic lock version")
    private Integer version;

    @Column(name = "deleted", type = ColumnType.BOOLEAN, comment = "Soft delete flag")
    private Boolean deleted;

    @Column(name = "created_by", type = ColumnType.VARCHAR, length = 64, comment = "Created by")
    private String createdBy;

    @Column(name = "created_at", type = ColumnType.TIMESTAMP, comment = "Created at")
    private Instant createdAt;

    @Column(name = "updated_by", type = ColumnType.VARCHAR, length = 64, comment = "Updated by")
    private String updatedBy;

    @Column(name = "updated_at", type = ColumnType.TIMESTAMP, comment = "Updated at")
    private Instant updatedAt;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Integer getVersion() {
        return version;
    }

    @Override
    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public Boolean getDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String getUpdatedBy() {
        return updatedBy;
    }

    @Override
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
