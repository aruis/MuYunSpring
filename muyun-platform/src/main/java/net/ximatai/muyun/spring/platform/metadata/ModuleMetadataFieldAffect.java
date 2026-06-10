package net.ximatai.muyun.spring.platform.metadata;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardSortableEntity;

@Getter
@Setter
@Table(name = "platform_module_metadata_field_affect", comment = "Module metadata field reference affect")
public class ModuleMetadataFieldAffect extends StandardSortableEntity {
    @Column(name = "module_metadata_field_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Module metadata field id")
    private String moduleMetadataFieldId;

    @Column(name = "reference_field_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Reference module metadata field id")
    private String referenceFieldId;

    @Column(name = "target_field_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Current form target module metadata field id")
    private String targetFieldId;
}
