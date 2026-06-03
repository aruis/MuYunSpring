package net.ximatai.muyun.spring.boot.web;

import java.util.function.Supplier;

public interface ScopedWeb<S> {
    S service();

    default <T> T webScope(Supplier<T> action) {
        return action.get();
    }
}
