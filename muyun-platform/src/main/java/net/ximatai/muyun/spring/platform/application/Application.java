package net.ximatai.muyun.spring.platform.application;

import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.spring.common.initialdata.InitialDataFields;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;

@Table(name = "platform_application", comment = "Platform application")
@InitialDataFields(operator = {"title", "enabled", "sortOrder"})
public class Application extends StandardEnabledSortableEntity {
    public String getAlias() {
        return getId();
    }

    public void setAlias(String alias) {
        setId(alias);
    }
}
