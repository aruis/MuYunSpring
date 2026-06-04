package net.ximatai.muyun.spring.platform.metadata;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.annotation.TrueOrFalse;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;

@Getter
@Setter
@Table(name = "platform_metadata_field", comment = "Platform metadata field")
public class MetadataField extends StandardEnabledSortableEntity {
    @Column(name = "metadata_id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Metadata id")
    private String metadataId;

    @Column(name = "field_name", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Field name")
    private String fieldName;

    @Column(name = "column_name", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Column name")
    private String columnName;

    @Column(name = "field_type_alias", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Field type alias")
    private String fieldTypeAlias;

    @Column(name = "required", type = ColumnType.BOOLEAN, comment = "Required flag",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean required = Boolean.FALSE;

    @Column(name = "unique_field", type = ColumnType.BOOLEAN, comment = "Unique field flag",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean uniqueField = Boolean.FALSE;

    @Column(name = "indexed", type = ColumnType.BOOLEAN, comment = "Indexed flag",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean indexed = Boolean.FALSE;

    @Column(name = "sortable_field", type = ColumnType.BOOLEAN, comment = "Sortable field flag",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean sortableField = Boolean.FALSE;

    @Column(name = "title_field", type = ColumnType.BOOLEAN, comment = "Title field flag",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean titleField = Boolean.FALSE;

}
