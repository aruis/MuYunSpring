package net.ximatai.muyun.spring.iam.position;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;

@Getter
@Setter
@Table(name = "iam_position", comment = "Position")
@CompositeIndex(columns = {"tenant_id", "code"}, unique = true)
public class Position extends StandardEnabledSortableEntity {
    @Column(name = "code", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Position code")
    private String code;

    @Column(name = "description", type = ColumnType.VARCHAR, length = 512, comment = "Description")
    private String description;
}
