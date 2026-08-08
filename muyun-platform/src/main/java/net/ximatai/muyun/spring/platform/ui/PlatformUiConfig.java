package net.ximatai.muyun.spring.platform.ui;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.annotation.TrueOrFalse;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;

@Getter
@Setter
@Table(name = "platform_ui_config", comment = "Platform low-code UI config")
@CompositeIndex(columns = {"ui_set_id", "client_type"}, unique = true)
@net.ximatai.muyun.spring.ability.SortPartitionBy(fields = "uiSetId")
public class PlatformUiConfig extends StandardEnabledSortableEntity {
    @Column(name = "ui_set_id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "UI set id")
    private String uiSetId;

    @Column(name = "client_type", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Client type")
    private PlatformUiClientType clientType = PlatformUiClientType.WEB;

    @Column(name = "layout_json", type = ColumnType.TEXT, comment = "Layout JSON")
    private String layoutJson;

    @Column(name = "scope_module_alias", type = ColumnType.VARCHAR, length = 128,
            comment = "Scoped list workspace selector module")
    private String scopeModuleAlias;

    @Column(name = "scope_field", type = ColumnType.VARCHAR, length = 64,
            comment = "Scoped list workspace consumer reference field")
    private String scopeField;

    @Column(name = "scope_query_criteria_key", type = ColumnType.VARCHAR, length = 64,
            comment = "Scoped list workspace external query criteria key")
    private String scopeQueryCriteriaKey;

    @Column(name = "scope_title", type = ColumnType.VARCHAR, length = 128,
            comment = "Scoped list workspace selector title")
    private String scopeTitle;

    @Column(name = "scope_search_placeholder", type = ColumnType.VARCHAR, length = 256,
            comment = "Scoped list workspace selector search placeholder")
    private String scopeSearchPlaceholder;

    @Column(name = "scope_show_item_subtitle", type = ColumnType.BOOLEAN,
            comment = "Show secondary text in scoped list selector", defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean scopeShowItemSubtitle = Boolean.FALSE;

    @Column(name = "scope_create_policy", type = ColumnType.VARCHAR, length = 32,
            comment = "Scoped list workspace create policy")
    private String scopeCreatePolicy;

    @Column(name = "published", type = ColumnType.BOOLEAN, comment = "Published config flag",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean published = Boolean.FALSE;
}
