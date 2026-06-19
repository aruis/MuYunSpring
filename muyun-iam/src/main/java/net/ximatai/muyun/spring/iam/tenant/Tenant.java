package net.ximatai.muyun.spring.iam.tenant;

import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;
import net.ximatai.muyun.spring.common.initialdata.InitialDataFields;

@Table(name = "iam_tenant", comment = "Tenant")
@InitialDataFields(operator = {"title", "enabled", "sortOrder"})
public class Tenant extends StandardEnabledSortableEntity {
    public String getAlias() {
        return getId();
    }

    public void setAlias(String alias) {
        setId(alias);
    }
}
