package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.spring.dynamic.metadata.FieldMoneyMode;

public record ModuleMetadataMoneyPrepareCommand(
        FieldMoneyMode currencyMode,
        String fixedCurrencyCode,
        String defaultCurrencyCode,
        String currencyFieldName,
        String baseAmountFieldName,
        String baseCurrencyCode,
        String rateTypeCode,
        String rateDateFieldId,
        String exchangeRateFieldName,
        Boolean createExchangeRateField,
        Boolean currencyRequired,
        String currencyFieldTypeAlias,
        String baseAmountFieldTypeAlias,
        String exchangeRateFieldTypeAlias
) {
}
