package net.ximatai.muyun.spring.platform.module;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Id;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledTreeEntity;

@Getter
@Setter
@Table(name = "platform_module", comment = "Platform module")
public class PlatformModule extends StandardEnabledTreeEntity {
    @Id
    @Column(name = "id", type = ColumnType.VARCHAR, length = 128, nullable = false, comment = "Module alias")
    private String id;

    @Column(name = "parent_id", type = ColumnType.VARCHAR, length = 128, comment = "Parent module alias")
    private String parentId;

    @Column(name = "application_alias", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Application alias")
    private String applicationAlias;

    @Column(name = "module_type", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Module type")
    private ModuleType moduleType;

    public String getAlias() {
        return getId();
    }

    public void setAlias(String alias) {
        setId(alias);
    }
}
