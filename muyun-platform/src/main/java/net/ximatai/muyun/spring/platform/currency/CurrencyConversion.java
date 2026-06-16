package net.ximatai.muyun.spring.platform.currency;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CurrencyConversion(
        String fromCurrencyCode,
        String toCurrencyCode,
        String rateTypeCode,
        LocalDate rateDate,
        BigDecimal originalAmount,
        BigDecimal exchangeRate,
        BigDecimal convertedAmount
) {
}
