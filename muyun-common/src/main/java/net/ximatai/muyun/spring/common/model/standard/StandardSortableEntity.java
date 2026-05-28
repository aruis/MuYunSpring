package net.ximatai.muyun.spring.common.model.standard;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.capability.SortCapable;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;

/**
 * Standard base class for titled entities that are ordered in a business scope.
 */
@Getter
@Setter
public abstract class StandardSortableEntity extends StandardTitledEntity implements SortCapable {
    @Column(name = PlatformAbilityFields.SORT_COLUMN, type = ColumnType.INT, comment = "Sort order")
    private Integer sortOrder;
}
