package net.ximatai.muyun.spring.common.di;

import java.util.function.Supplier;
import java.util.stream.Stream;

public interface ObjectProvider<T> {
    default T getObject() {
        T value = getIfAvailable();
        if (value == null) {
            throw new IllegalStateException("required bean is not available");
        }
        return value;
    }

    default T getObject(Object... args) {
        return getObject();
    }

    T getIfAvailable();

    default T getIfUnique() {
        return getIfAvailable();
    }

    default T getIfAvailable(Supplier<? extends T> defaultSupplier) {
        T value = getIfAvailable();
        if (value != null) {
            return value;
        }
        return defaultSupplier == null ? null : defaultSupplier.get();
    }

    default Stream<T> orderedStream() {
        T value = getIfAvailable();
        return value == null ? Stream.empty() : Stream.of(value);
    }
}
