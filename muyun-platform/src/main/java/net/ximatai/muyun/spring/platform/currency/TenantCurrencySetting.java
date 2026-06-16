package net.ximatai.muyun.spring.platform.currency;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardTitledEntity;

@Getter
@Setter
@Table(name = "platform_tenant_currency_setting", comment = "Platform tenant currency setting")
@CompositeIndex(columns = {"tenant_id"}, unique = true)
public class TenantCurrencySetting extends StandardTitledEntity {
    @Column(name = "base_currency_code", type = ColumnType.VARCHAR, length = 3, nullable = false,
            comment = "Tenant base currency code")
    private String baseCurrencyCode;
}
