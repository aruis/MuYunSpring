package net.ximatai.muyun.spring.platform.application;

import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;

@Table(name = "platform_application", comment = "Platform application")
public class Application extends StandardEnabledSortableEntity {
    public String getAlias() {
        return getId();
    }

    public void setAlias(String alias) {
        setId(alias);
    }
}
