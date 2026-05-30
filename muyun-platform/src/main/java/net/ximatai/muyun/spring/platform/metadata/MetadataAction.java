package net.ximatai.muyun.spring.platform.metadata;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionKind;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;

@Getter
@Setter
@Table(name = "platform_metadata_action", comment = "Metadata action")
@CompositeIndex(columns = {"relation_id", "action_code"}, unique = true)
public class MetadataAction extends StandardEnabledSortableEntity {
    @Column(name = "relation_id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Module metadata relation id")
    private String relationId;

    @Column(name = "action_code", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Action code")
    private String actionCode;

    @Column(name = "action_kind", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Action kind")
    private EntityActionKind actionKind;

    @Column(name = "action_level", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Action level")
    private EntityActionLevel actionLevel;

    @Column(name = "permission_code", type = ColumnType.VARCHAR, length = 128, comment = "Permission code")
    private String permissionCode;
}
