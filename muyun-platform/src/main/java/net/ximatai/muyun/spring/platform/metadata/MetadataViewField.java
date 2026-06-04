package net.ximatai.muyun.spring.platform.metadata;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.annotation.TrueOrFalse;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;
import net.ximatai.muyun.spring.dynamic.metadata.ViewControlType;

@Getter
@Setter
@Table(name = "platform_metadata_view_field", comment = "Metadata view field")
@CompositeIndex(columns = {"view_id", "metadata_field_id"}, unique = true)
public class MetadataViewField extends StandardEnabledSortableEntity {
    @Column(name = "view_id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Metadata view id")
    private String viewId;

    @Column(name = "metadata_field_id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Metadata field id")
    private String metadataFieldId;

    @Column(name = "visible", type = ColumnType.BOOLEAN, comment = "Visible flag",
            defaultVal = @Default(bool = TrueOrFalse.TRUE))
    private Boolean visible = Boolean.TRUE;

    @Column(name = "control_type", type = ColumnType.VARCHAR, length = 32, comment = "View control type")
    private ViewControlType controlType;

    @Column(name = "read_only", type = ColumnType.BOOLEAN, comment = "Read only flag",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean readOnly = Boolean.FALSE;

    @Column(name = "required_override", type = ColumnType.BOOLEAN, comment = "Required override")
    private Boolean requiredOverride;
}
