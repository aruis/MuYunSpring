package net.ximatai.muyun.spring.platform.metadata;

public record ModuleMetadataMoneyPrepareResult(
        ModuleMetadataField moduleField,
        MetadataField currencyField,
        MetadataField baseAmountField,
        MetadataField exchangeRateField
) {
}
