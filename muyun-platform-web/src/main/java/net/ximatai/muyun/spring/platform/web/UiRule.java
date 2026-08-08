package net.ximatai.muyun.spring.platform.web;

public record UiRule<T>(T constant, UiFormula formula, String disabledHint) {
    public UiRule(T constant) {
        this(constant, null, null);
    }
    public UiRule(T constant, UiFormula formula) {
        this(constant, formula, null);
    }

    public static <T> UiRule<T> constant(T value) {
        return new UiRule<>(value);
    }

    public static UiRule<Boolean> formula(UiFormula formula) {
        return new UiRule<>(null, formula);
    }
}
