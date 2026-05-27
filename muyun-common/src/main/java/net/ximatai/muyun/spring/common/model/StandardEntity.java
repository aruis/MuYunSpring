package net.ximatai.muyun.spring.common.model;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Id;
import net.ximatai.muyun.database.core.builder.ColumnType;

import java.time.Instant;

@Getter
@Setter
public abstract class StandardEntity implements EntityContract {
    @Id
    @Column(name = "id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "ID")
    private String id;

    @Column(name = "tenant_id", type = ColumnType.VARCHAR, length = 64, comment = "Tenant id")
    private String tenantId;

    @Column(name = "version", type = ColumnType.INT, comment = "Optimistic lock version")
    private Integer version;

    @Column(name = "deleted", type = ColumnType.BOOLEAN, comment = "Soft delete flag")
    private Boolean deleted;

    @Column(name = "deleted_at", type = ColumnType.TIMESTAMP, comment = "Deleted at")
    private Instant deletedAt;

    @Column(name = "created_by", type = ColumnType.VARCHAR, length = 64, comment = "Created by")
    private String createdBy;

    @Column(name = "created_at", type = ColumnType.TIMESTAMP, comment = "Created at")
    private Instant createdAt;

    @Column(name = "updated_by", type = ColumnType.VARCHAR, length = 64, comment = "Updated by")
    private String updatedBy;

    @Column(name = "updated_at", type = ColumnType.TIMESTAMP, comment = "Updated at")
    private Instant updatedAt;
}
