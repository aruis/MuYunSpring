package net.ximatai.muyun.spring.platform.menu;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledTreeEntity;

@Getter
@Setter
@Table(name = "platform_menu", comment = "Platform menu")
public class Menu extends StandardEnabledTreeEntity {
    @Column(name = "scheme_id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Menu scheme id")
    private String schemeId;

    @Column(name = "menu_type", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Menu type")
    private MenuType menuType;

    @Column(name = "module_alias", type = ColumnType.VARCHAR, length = 128, comment = "Target module alias")
    private String moduleAlias;

    @Column(name = "route", type = ColumnType.VARCHAR, length = 256, comment = "Route path")
    private String route;

    @Column(name = "external_url", type = ColumnType.VARCHAR, length = 512, comment = "External url")
    private String externalUrl;
}
