package net.ximatai.muyun.spring.platform.metadata;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
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

    @Column(name = "required", type = ColumnType.BOOLEAN, comment = "Required flag")
    private Boolean required;

    @Column(name = "unique_field", type = ColumnType.BOOLEAN, comment = "Unique field flag")
    private Boolean uniqueField;

    @Column(name = "indexed", type = ColumnType.BOOLEAN, comment = "Indexed flag")
    private Boolean indexed;

    @Column(name = "sortable_field", type = ColumnType.BOOLEAN, comment = "Sortable field flag")
    private Boolean sortableField;

    @Column(name = "title_field", type = ColumnType.BOOLEAN, comment = "Title field flag")
    private Boolean titleField;

}
