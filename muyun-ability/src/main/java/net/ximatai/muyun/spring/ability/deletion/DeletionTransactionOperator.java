package net.ximatai.muyun.spring.ability.deletion;

import java.util.function.Supplier;

/** Opens one transaction for a complete standard deletion tree when available. */
@FunctionalInterface
public interface DeletionTransactionOperator {
    DeletionTransactionOperator NONE = Supplier::get;

    <T> T execute(Supplier<T> work);
}
