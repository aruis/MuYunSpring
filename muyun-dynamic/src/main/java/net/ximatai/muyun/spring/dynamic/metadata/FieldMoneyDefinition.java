package net.ximatai.muyun.spring.dynamic.metadata;

public record FieldMoneyDefinition(
        FieldMoneyMode currencyMode,
        String fixedCurrencyCode,
        String defaultCurrencyCode,
        String currencyFieldName,
        String baseAmountFieldName,
        String baseCurrencyCode,
        String rateTypeCode,
        String rateDateFieldName,
        String exchangeRateFieldName,
        boolean currencyRequired
) {
    public FieldMoneyDefinition {
        fixedCurrencyCode = upperOrNull(fixedCurrencyCode);
        defaultCurrencyCode = upperOrNull(defaultCurrencyCode);
        baseCurrencyCode = upperOrNull(baseCurrencyCode);
        rateTypeCode = upperOrNull(rateTypeCode);
        currencyFieldName = textOrNull(currencyFieldName);
        baseAmountFieldName = textOrNull(baseAmountFieldName);
        rateDateFieldName = textOrNull(rateDateFieldName);
        exchangeRateFieldName = textOrNull(exchangeRateFieldName);
    }

    public static final FieldMoneyDefinition NONE = new FieldMoneyDefinition(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false
    );

    public boolean enabled() {
        return baseAmountFieldName != null && !baseAmountFieldName.isBlank();
    }

    private static String upperOrNull(String value) {
        String text = textOrNull(value);
        return text == null ? null : text.toUpperCase();
    }

    private static String textOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
