package net.ximatai.muyun.spring.ability.reference;

import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
public interface ModuleProperty<T, R> extends Function<T, R>, Serializable {
}
