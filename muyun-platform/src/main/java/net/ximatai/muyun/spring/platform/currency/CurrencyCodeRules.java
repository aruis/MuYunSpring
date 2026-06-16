package net.ximatai.muyun.spring.platform.currency;

import java.util.Locale;

public final class CurrencyCodeRules {
    private CurrencyCodeRules() {
    }

    public static String normalizeCurrencyCode(String value) {
        return normalizeUpper(value);
    }

    public static String normalizeRateTypeCode(String value) {
        return normalizeUpper(value);
    }

    public static boolean isCurrencyCode(String value) {
        String code = normalizeCurrencyCode(value);
        return code != null && code.matches("[A-Z]{3}");
    }

    public static boolean isRateTypeCode(String value) {
        String code = normalizeRateTypeCode(value);
        return code != null && code.matches("[A-Z][A-Z0-9_]{0,63}");
    }

    private static String normalizeUpper(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
