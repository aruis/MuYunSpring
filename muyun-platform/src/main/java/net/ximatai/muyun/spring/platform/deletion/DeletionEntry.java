package net.ximatai.muyun.spring.platform.deletion;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;

import java.time.Instant;

/**
 * One resource affected by a {@link DeletionOperation}.
 */
@Getter
@Setter
@Table(name = "platform_deletion_entry", comment = "Deletion lifecycle affected resource")
@CompositeIndex(columns = {"operation_id", "parent_entry_id"})
@CompositeIndex(columns = {"tenant_id", "resource_module_alias", "resource_record_id", "status"})
@CompositeIndex(columns = {"source_entry_id"})
public class DeletionEntry extends StandardEntity {
    @Column(name = "operation_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Deletion lifecycle operation id")
    private String operationId;

    @Column(name = "parent_entry_id", type = ColumnType.VARCHAR, length = 32,
            comment = "Parent affected resource entry id")
    private String parentEntryId;

    @Column(name = "source_entry_id", type = ColumnType.VARCHAR, length = 32,
            comment = "Original delete entry id for restore or purge")
    private String sourceEntryId;

    @Column(name = "resource_module_alias", type = ColumnType.VARCHAR, length = 128, nullable = false,
            comment = "Affected resource module alias")
    private String resourceModuleAlias;

    @Column(name = "resource_entity_alias", type = ColumnType.VARCHAR, length = 64,
            comment = "Affected resource entity alias")
    private String resourceEntityAlias;

    @Column(name = "resource_record_id", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "Affected resource record id")
    private String resourceRecordId;

    @Column(name = "trigger_type", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Affected resource trigger type")
    private DeletionEntryTrigger triggerType;

    @Column(name = "delete_mode", type = ColumnType.VARCHAR, length = 32,
            comment = "Mode selected by the resource module")
    private DeletionEntryMode deleteMode;

    @Column(name = "status", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Affected resource execution status")
    private DeletionEntryStatus status;

    @Column(name = "resource_version", type = ColumnType.INT,
            comment = "Resource version captured before lifecycle command")
    private Integer resourceVersion;

    @Column(name = "display_snapshot", type = ColumnType.TEXT,
            comment = "Resource display snapshot for recycle-bin and audit views")
    private String displaySnapshot;

    @Column(name = "result_message", type = ColumnType.TEXT, comment = "Affected resource result message")
    private String resultMessage;

    @Column(name = "started_at", type = ColumnType.TIMESTAMP, nullable = false,
            comment = "Affected resource handling started at")
    private Instant startedAt;

    @Column(name = "completed_at", type = ColumnType.TIMESTAMP, comment = "Affected resource handling completed at")
    private Instant completedAt;
}
