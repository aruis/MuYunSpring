package net.ximatai.muyun.spring.common.model.standard;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;

/**
 * Standard base class for tree entities that can be enabled or disabled.
 */
@Getter
@Setter
public abstract class StandardEnabledTreeEntity extends StandardTreeEntity implements EnabledCapable {
    @Column(name = PlatformAbilityFields.ENABLED_COLUMN, type = ColumnType.BOOLEAN, comment = "Enabled flag")
    private Boolean enabled;
}
