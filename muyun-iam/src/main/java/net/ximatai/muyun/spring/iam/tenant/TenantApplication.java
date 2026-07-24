package net.ximatai.muyun.spring.iam.tenant;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardTitledEntity;

/** A tenant's product-level application entitlement. */
@Getter
@Setter
@Table(name = "iam_tenant_application", comment = "Tenant application entitlement")
public class TenantApplication extends StandardTitledEntity {
    @Column(name = "application_alias", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "Application alias")
    private String applicationAlias;
}
