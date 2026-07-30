package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;

/**
 * The business partition in which sortable records share one order sequence.
 *
 * <p>A partition is deliberately responsible for both selecting its records and
 * deciding whether two records belong together. Keeping those two operations in
 * one contract prevents a reorder from querying a different set of records than
 * the set accepted by {@link SortAbility}.</p>
 */
public interface SortPartition<T> {
    Criteria criteriaFor(T entity);

    void requireSamePartition(T left, T right);
}
