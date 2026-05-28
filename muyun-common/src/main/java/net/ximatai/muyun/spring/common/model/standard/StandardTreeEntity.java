package net.ximatai.muyun.spring.common.model.standard;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.capability.TreeCapable;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;

/**
 * Standard base class for sortable titled entities arranged in a tree.
 */
@Getter
@Setter
public abstract class StandardTreeEntity extends StandardSortableEntity implements TreeCapable {
    @Column(name = PlatformAbilityFields.TREE_PARENT_COLUMN, type = ColumnType.VARCHAR, length = PlatformAbilityFields.TREE_PARENT_LENGTH, comment = "Parent ID")
    private String parentId;
}
