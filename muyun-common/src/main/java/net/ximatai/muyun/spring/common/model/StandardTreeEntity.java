package net.ximatai.muyun.spring.common.model;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;

@Getter
@Setter
public abstract class StandardTreeEntity extends StandardEntity implements TreeCapable, EnabledCapable, TitledCapable {
    @Column(name = PlatformAbilityFields.TREE_PARENT_COLUMN, type = ColumnType.VARCHAR, length = PlatformAbilityFields.TREE_PARENT_LENGTH, comment = "Parent ID")
    private String parentId;

    @Column(name = PlatformAbilityFields.TITLE_COLUMN, type = ColumnType.VARCHAR, length = PlatformAbilityFields.TITLE_LENGTH, nullable = false, comment = "Title")
    private String title;

    @Column(name = PlatformAbilityFields.SORT_COLUMN, type = ColumnType.INT, comment = "Sort order")
    private Integer sortOrder;

    @Column(name = PlatformAbilityFields.ENABLED_COLUMN, type = ColumnType.BOOLEAN, comment = "Enabled flag")
    private Boolean enabled;
}
