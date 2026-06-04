package net.ximatai.muyun.spring.platform.menu;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;

@Getter
@Setter
@Table(name = "platform_menu_scheme", comment = "Platform menu scheme")
public class MenuScheme extends StandardEnabledSortableEntity {
    @Column(name = "alias", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Menu scheme alias")
    private String alias;

    @Column(name = "scope_type", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Menu scheme scope type", defaultVal = @Default(varchar = "tenant"))
    private MenuScopeType scopeType = MenuScopeType.TENANT;

    @Column(name = "scope_id", type = ColumnType.VARCHAR, length = 64, comment = "Menu scheme scope id")
    private String scopeId;
}
