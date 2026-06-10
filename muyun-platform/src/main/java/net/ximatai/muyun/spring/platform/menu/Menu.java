package net.ximatai.muyun.spring.platform.menu;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledTreeEntity;

@Getter
@Setter
@Table(name = "platform_menu", comment = "Platform menu")
public class Menu extends StandardEnabledTreeEntity {
    @Column(name = "scheme_id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Menu scheme id")
    private String schemeId;

    @Column(name = "menu_type", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Menu type",
            defaultVal = @Default(varchar = "group"))
    private MenuType menuType = MenuType.GROUP;

    @Column(name = "module_alias", type = ColumnType.VARCHAR, length = 128, comment = "Target module alias")
    private String moduleAlias;

    @Column(name = "route", type = ColumnType.VARCHAR, length = 256, comment = "Route path")
    private String route;

    @Column(name = "external_url", type = ColumnType.VARCHAR, length = 512, comment = "External url")
    private String externalUrl;

    @Column(name = "page_mode", type = ColumnType.VARCHAR, length = 32, comment = "Low-code page mode")
    private MenuPageMode pageMode;

    @Column(name = "default_ui_config_id", type = ColumnType.VARCHAR, length = 32, comment = "Default UI config id")
    private String defaultUiConfigId;

    @Column(name = "default_query_template_id", type = ColumnType.VARCHAR, length = 32,
            comment = "Default query template id")
    private String defaultQueryTemplateId;

    @Column(name = "entry_params_json", type = ColumnType.TEXT, comment = "Entry params JSON")
    private String entryParamsJson;
}
