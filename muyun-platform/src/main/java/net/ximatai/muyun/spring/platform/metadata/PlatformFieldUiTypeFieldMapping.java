package net.ximatai.muyun.spring.platform.metadata;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardSortableEntity;

@Getter
@Setter
@Table(name = "platform_field_ui_type_field_mapping", comment = "Platform field UI type field mapping")
@CompositeIndex(columns = {"field_ui_type_alias", "source_key"}, unique = true)
public class PlatformFieldUiTypeFieldMapping extends StandardSortableEntity {
    @Column(name = "field_ui_type_alias", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "Field UI type alias")
    private String fieldUiTypeAlias;

    @Column(name = "source_key", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Source key")
    private String sourceKey;
}
