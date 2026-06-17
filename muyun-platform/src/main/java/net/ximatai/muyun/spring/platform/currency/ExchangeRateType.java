package net.ximatai.muyun.spring.platform.currency;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;

@Getter
@Setter
@Table(name = "platform_exchange_rate_type", comment = "Platform exchange rate type")
@CompositeIndex(columns = {"tenant_id", "code"}, unique = true)
public class ExchangeRateType extends StandardEnabledSortableEntity {
    @Column(name = "code", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "Exchange rate type code")
    private String code;

    @Column(name = "system_managed", type = ColumnType.BOOLEAN, comment = "System managed flag")
    private Boolean systemManaged = Boolean.FALSE;
}
