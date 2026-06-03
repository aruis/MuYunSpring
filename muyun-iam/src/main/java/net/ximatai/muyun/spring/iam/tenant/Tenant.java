package net.ximatai.muyun.spring.iam.tenant;

import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;

@Table(name = "iam_tenant", comment = "Tenant")
public class Tenant extends StandardEnabledSortableEntity {
    public String getAlias() {
        return getId();
    }

    public void setAlias(String alias) {
        setId(alias);
    }
}
