package net.ximatai.muyun.spring.platform.generation;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Indexed;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardSortableEntity;

@Getter
@Setter
@Table(name = "platform_record_generation_split_group_field", comment = "Record generation split group field")
public class RecordGenerationSplitGroupField extends StandardSortableEntity {
    @Indexed
    @Column(name = "split_policy_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Split policy id")
    private String splitPolicyId;

    @Column(name = "field_name", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "Group field name")
    private String fieldName;
}
