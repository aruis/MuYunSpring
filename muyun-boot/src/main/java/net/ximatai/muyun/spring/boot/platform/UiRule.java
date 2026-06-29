package net.ximatai.muyun.spring.boot.platform;

public record UiRule<T>(T constant) {
    public static <T> UiRule<T> constant(T value) {
        return new UiRule<>(value);
    }
}
