package net.ximatai.muyun.spring.common.option;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

/** Resolves persisted or materialized option values to the exact code used by an option source. */
public final class OptionValueCodeResolver {
    private OptionValueCodeResolver() {
    }

    public static String resolve(OptionBinding binding, Object value) {
        if (value instanceof CodeTitleEnum codeTitleEnum) {
            return codeTitleEnum.getCode();
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.isBlank()) {
                return null;
            }
            return enumConstantCode(binding, trimmed);
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return null;
    }

    private static String enumConstantCode(OptionBinding binding, String value) {
        if (binding == null || !OptionBinding.ENUM_SOURCE.equals(binding.sourceType())) {
            return value;
        }
        Class<?> enumType = loadEnumType(binding.source());
        try {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Enum<?> constant = Enum.valueOf((Class) enumType, value);
            return constant instanceof CodeTitleEnum codeTitleEnum ? codeTitleEnum.getCode() : value;
        } catch (IllegalArgumentException ignored) {
            // The value may already be the business code. Preserve exact matching; never fold case.
            return value;
        }
    }

    private static Class<?> loadEnumType(String className) {
        try {
            Class<?> type = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            if (!type.isEnum() || !CodeTitleEnum.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException("enum option binding requires CodeTitleEnum: " + className);
            }
            return type;
        } catch (ClassNotFoundException exception) {
            throw new IllegalArgumentException("unknown enum option type: " + className, exception);
        }
    }
}
