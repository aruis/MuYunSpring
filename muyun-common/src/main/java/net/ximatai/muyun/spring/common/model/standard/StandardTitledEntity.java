package net.ximatai.muyun.spring.common.model.standard;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.capability.TitledCapable;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.model.title.TitleField;

/**
 * Standard base class for entities represented by a display title.
 */
@Getter
@Setter
public abstract class StandardTitledEntity extends StandardEntity implements TitledCapable {
    @TitleField
    @Column(name = PlatformAbilityFields.TITLE_COLUMN, type = ColumnType.VARCHAR, length = PlatformAbilityFields.TITLE_LENGTH, nullable = false, comment = "Title")
    private String title;
}
