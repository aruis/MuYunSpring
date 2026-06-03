package net.ximatai.muyun.spring.platform.metadata;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;

@Getter
@Setter
@Table(name = "platform_metadata", comment = "Platform metadata")
public class Metadata extends StandardEnabledSortableEntity {
    @Column(name = "application_alias", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Application alias")
    private String applicationAlias;

    @Column(name = "alias", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Metadata alias")
    private String alias;

    @Column(name = "schema_name", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Physical schema name")
    private String schemaName;

    @Column(name = "table_name", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Physical table name")
    private String tableName;

    @Column(name = "data_scope_enabled", type = ColumnType.BOOLEAN, comment = "Data scope enabled")
    private Boolean dataScopeEnabled;
}
