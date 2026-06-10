package net.ximatai.muyun.spring.platform.ui;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;

@Getter
@Setter
@Table(name = "platform_record_navigation_session", comment = "Platform record navigation session")
@CompositeIndex(columns = {"tenant_id", "user_id", "module_alias", "entity_alias"})
public class PlatformRecordNavigationSession extends StandardEntity {
    @Column(name = "user_id", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "User id")
    private String userId;

    @Column(name = "module_alias", type = ColumnType.VARCHAR, length = 128, nullable = false,
            comment = "Business module alias")
    private String moduleAlias;

    @Column(name = "entity_alias", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "Dynamic entity alias")
    private String entityAlias;

    @Column(name = "record_ids", type = ColumnType.TEXT, nullable = false, comment = "Ordered record ids")
    private String recordIds;

    @Column(name = "page_num", type = ColumnType.INT, nullable = false, comment = "Page number")
    private Integer pageNum;

    @Column(name = "page_size", type = ColumnType.INT, nullable = false, comment = "Page size")
    private Integer pageSize;

    @Column(name = "total_count", type = ColumnType.BIGINT, comment = "Total count")
    private Long total;

    @Column(name = "query_snapshot_key", type = ColumnType.VARCHAR, length = 128, comment = "Query snapshot key")
    private String querySnapshotKey;
}
