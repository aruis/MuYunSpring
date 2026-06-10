package net.ximatai.muyun.spring.platform.metadata;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;
import net.ximatai.muyun.spring.dynamic.metadata.ViewControlType;

@Getter
@Setter
@Table(name = "platform_field_ui_type", comment = "Platform field UI type")
@CompositeIndex(columns = {"alias"}, unique = true)
public class PlatformFieldUiType extends StandardEnabledSortableEntity {
    @Column(name = "alias", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Field UI type alias")
    private String alias;

    @Column(name = "default_field_type_alias", type = ColumnType.VARCHAR, length = 64, comment = "Default field type alias")
    private String defaultFieldTypeAlias;

    @Column(name = "control_type", type = ColumnType.VARCHAR, length = 32, comment = "Compatible view control type")
    private ViewControlType controlType;

    @Column(name = "icon", type = ColumnType.VARCHAR, length = 128, comment = "Icon")
    private String icon;
}
