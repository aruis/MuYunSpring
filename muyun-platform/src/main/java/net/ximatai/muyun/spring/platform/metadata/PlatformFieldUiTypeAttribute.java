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
@Table(name = "platform_field_ui_type_attribute", comment = "Platform field UI type attribute")
@CompositeIndex(columns = {"field_ui_type_alias", "attribute_alias"}, unique = true)
public class PlatformFieldUiTypeAttribute extends StandardSortableEntity {
    @Column(name = "field_ui_type_alias", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "Field UI type alias")
    private String fieldUiTypeAlias;

    @Column(name = "attribute_alias", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "Attribute alias")
    private String attributeAlias;

    @Column(name = "value_field_type_alias", type = ColumnType.VARCHAR, length = 64, comment = "Value field type alias")
    private String valueFieldTypeAlias;

    @Column(name = "default_value", type = ColumnType.VARCHAR, length = 512, comment = "Default value")
    private String defaultValue;
}
