package net.ximatai.muyun.spring.platform.initialdata;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class InitialDataField<T> {
    private final String name;
    private final Function<T, ?> reader;
    private final BiConsumer<T, T> copier;

    private InitialDataField(String name, Function<T, ?> reader, BiConsumer<T, T> copier) {
        this.name = requireText(name, "fieldName");
        this.reader = Objects.requireNonNull(reader, "reader must not be null");
        this.copier = Objects.requireNonNull(copier, "copier must not be null");
    }

    public static <T, V> InitialDataField<T> of(String name, Function<T, V> reader, BiConsumer<T, V> writer) {
        Objects.requireNonNull(writer, "writer must not be null");
        return new InitialDataField<>(name, reader, (target, source) -> writer.accept(target, reader.apply(source)));
    }

    public String name() {
        return name;
    }

    public Object value(T source) {
        return source == null ? null : reader.apply(source);
    }

    public boolean differs(T left, T right) {
        return !Objects.equals(value(left), value(right));
    }

    public void copy(T target, T source) {
        copier.accept(target, source);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
