package net.ximatai.muyun.spring.platform.ui;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;

@Getter
@Setter
@Table(name = "platform_page_preference", comment = "Platform page user preference")
@CompositeIndex(columns = {"tenant_id", "user_id", "module_alias", "client_type", "page_key"}, unique = true)
public class PlatformPagePreference extends StandardEntity {
    @Column(name = "user_id", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "User id")
    private String userId;

    @Column(name = "module_alias", type = ColumnType.VARCHAR, length = 128, nullable = false,
            comment = "Business module alias")
    private String moduleAlias;

    @Column(name = "client_type", type = ColumnType.VARCHAR, length = 16, nullable = false, comment = "Client type")
    private String clientType;

    @Column(name = "page_key", type = ColumnType.VARCHAR, length = 128, nullable = false, comment = "Page key")
    private String pageKey;

    @Column(name = "preference_json", type = ColumnType.TEXT, nullable = false, comment = "Preference JSON")
    private String preferenceJson;
}
