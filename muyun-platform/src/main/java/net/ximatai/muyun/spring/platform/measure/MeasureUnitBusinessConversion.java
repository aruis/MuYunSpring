package net.ximatai.muyun.spring.platform.measure;

import java.math.BigDecimal;
import java.util.List;

public record MeasureUnitBusinessConversion(
        MeasureUnitConversionContext context,
        BigDecimal value,
        String fromCategoryAlias,
        String fromUnitCode,
        String toCategoryAlias,
        String toUnitCode,
        BigDecimal convertedValue,
        List<String> ruleIds
) {
}
