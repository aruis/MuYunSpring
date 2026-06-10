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
@Table(name = "platform_query_template", comment = "Platform query template")
@CompositeIndex(columns = {"module_alias", "alias"}, unique = true)
public class PlatformQueryTemplate extends StandardEnabledSortableEntity {
    @Column(name = "module_alias", type = ColumnType.VARCHAR, length = 128, nullable = false, comment = "Module alias")
    private String moduleAlias;

    @Column(name = "alias", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Query template alias")
    private String alias;

    @Column(name = "default_template", type = ColumnType.BOOLEAN, comment = "Default query template",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean defaultTemplate = Boolean.FALSE;
}
