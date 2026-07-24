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
 * Immutable audit envelope for one delete, restore, or purge command.
 *
 * <p>Its entries form the resource impact tree. The business tables continue
 * to own their actual soft- or hard-delete implementation.</p>
 */
@Getter
@Setter
@Table(name = "platform_deletion_operation", comment = "Deletion lifecycle operation")
@CompositeIndex(columns = {"tenant_id", "root_module_alias", "root_record_id", "started_at"})
@CompositeIndex(columns = {"tenant_id", "operation_type", "status", "started_at"})
@CompositeIndex(columns = {"operator_id", "started_at"})
public class DeletionOperation extends StandardEntity {
    @Column(name = "operation_type", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Deletion lifecycle operation type")
    private DeletionOperationType operationType;

    @Column(name = "status", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Deletion lifecycle operation status")
    private DeletionOperationStatus status;

    @Column(name = "root_module_alias", type = ColumnType.VARCHAR, length = 128, nullable = false,
            comment = "Root resource module alias")
    private String rootModuleAlias;

    @Column(name = "root_entity_alias", type = ColumnType.VARCHAR, length = 64,
            comment = "Root resource entity alias")
    private String rootEntityAlias;

    @Column(name = "root_record_id", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "Root resource record id")
    private String rootRecordId;

    @Column(name = "source_operation_id", type = ColumnType.VARCHAR, length = 32,
            comment = "Original delete operation id for restore or purge")
    private String sourceOperationId;

    @Column(name = "operator_id", type = ColumnType.VARCHAR, length = 64,
            comment = "Lifecycle operation operator id")
    private String operatorId;

    @Column(name = "reason", type = ColumnType.TEXT, comment = "Lifecycle operation reason")
    private String reason;

    @Column(name = "result_message", type = ColumnType.TEXT, comment = "Lifecycle operation result message")
    private String resultMessage;

    @Column(name = "started_at", type = ColumnType.TIMESTAMP, nullable = false,
            comment = "Lifecycle operation started at")
    private Instant startedAt;

    @Column(name = "completed_at", type = ColumnType.TIMESTAMP, comment = "Lifecycle operation completed at")
    private Instant completedAt;
}
