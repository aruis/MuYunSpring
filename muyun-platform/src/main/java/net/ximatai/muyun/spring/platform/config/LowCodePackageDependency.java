package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.currency.CurrencyCodeRules;
import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategoryService;

public record LowCodePackageDependency(
        LowCodePackageDependencyType type,
        String applicationAlias,
        String moduleAlias,
        String alias,
        boolean required
) {
    public LowCodePackageDependency {
        if (type == null) {
            throw new IllegalArgumentException("dependency type must not be null");
        }
        applicationAlias = normalize(applicationAlias);
        moduleAlias = normalize(moduleAlias);
        alias = normalize(alias);
        if (type == LowCodePackageDependencyType.CURRENCY) {
            alias = CurrencyCodeRules.normalizeCurrencyCode(alias);
        }
        if (type == LowCodePackageDependencyType.EXCHANGE_RATE_TYPE) {
            alias = CurrencyCodeRules.normalizeRateTypeCode(alias);
        }
        if (applicationAlias != null) {
            PlatformNameRules.requireApplicationAlias(applicationAlias);
        }
        if (moduleAlias != null) {
            PlatformNameRules.requireModuleAlias(moduleAlias);
        }
        validateAlias(type, alias);
    }

    public static LowCodePackageDependency module(String moduleAlias) {
        return new LowCodePackageDependency(LowCodePackageDependencyType.MODULE, null, moduleAlias, null, true);
    }

    public static LowCodePackageDependency action(String moduleAlias, String actionCode) {
        return new LowCodePackageDependency(LowCodePackageDependencyType.ACTION, null, moduleAlias, actionCode, true);
    }

    public static LowCodePackageDependency dictionary(String applicationAlias, String dictionaryAlias) {
        return new LowCodePackageDependency(LowCodePackageDependencyType.DICTIONARY,
                applicationAlias, null, dictionaryAlias, true);
    }

    public static LowCodePackageDependency measureUnit(String applicationAlias, String categoryAlias) {
        return new LowCodePackageDependency(LowCodePackageDependencyType.MEASURE_UNIT,
                applicationAlias, null, categoryAlias, true);
    }

    public static LowCodePackageDependency sharedMeasureUnit(String categoryAlias) {
        return measureUnit(MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS, categoryAlias);
    }

    public static LowCodePackageDependency currency(String currencyCode) {
        return new LowCodePackageDependency(LowCodePackageDependencyType.CURRENCY,
                null, null, currencyCode, true);
    }

    public static LowCodePackageDependency exchangeRateType(String rateTypeCode) {
        return new LowCodePackageDependency(LowCodePackageDependencyType.EXCHANGE_RATE_TYPE,
                null, null, rateTypeCode, true);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static void validateAlias(LowCodePackageDependencyType type, String alias) {
        if (alias == null) {
            return;
        }
        if (type == LowCodePackageDependencyType.CURRENCY) {
            if (!CurrencyCodeRules.isCurrencyCode(alias)) {
                throw new IllegalArgumentException("dependency currency code must be ISO 4217 alpha-3 code: " + alias);
            }
            return;
        }
        if (type == LowCodePackageDependencyType.EXCHANGE_RATE_TYPE) {
            if (!CurrencyCodeRules.isRateTypeCode(alias)) {
                throw new IllegalArgumentException("dependency exchange rate type must use upper snake code: " + alias);
            }
            return;
        }
        if (type == LowCodePackageDependencyType.ACTION) {
            PlatformNameRules.requireActionCode(alias, "dependency actionCode");
            return;
        }
        PlatformNameRules.requireIdentifier(alias, "dependency alias");
    }
}
