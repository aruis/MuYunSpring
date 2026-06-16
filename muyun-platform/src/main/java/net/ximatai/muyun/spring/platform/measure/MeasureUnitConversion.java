package net.ximatai.muyun.spring.platform.measure;

import java.math.BigDecimal;

public record MeasureUnitConversion(
        String applicationAlias,
        String categoryAlias,
        BigDecimal value,
        String fromUnitCode,
        String toUnitCode,
        BigDecimal baseValue,
        BigDecimal convertedValue
) {
}
