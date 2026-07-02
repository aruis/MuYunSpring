package net.ximatai.muyun.spring.common.di;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class ObjectProviders {
    private ObjectProviders() {
    }

    public static <T> ObjectProvider<T> of(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getIfAvailable() {
                return value;
            }
        };
    }

    public static <T> ObjectProvider<T> of(List<T> values) {
        List<T> copy = values == null ? List.of() : List.copyOf(values);
        return new ObjectProvider<>() {
            @Override
            public T getIfAvailable() {
                return copy.isEmpty() ? null : copy.get(0);
            }

            @Override
            public Stream<T> orderedStream() {
                return copy.stream().filter(Objects::nonNull);
            }
        };
    }
}
