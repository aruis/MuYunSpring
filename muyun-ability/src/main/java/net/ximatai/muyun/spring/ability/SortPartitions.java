package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/** Factory methods for the standard sortable-record partition shapes. */
public final class SortPartitions {
    private static final SortPartition<Object> GLOBAL = new SortPartition<>() {
        @Override
        public Criteria criteriaFor(Object entity) {
            return Criteria.of();
        }

        @Override
        public void requireSamePartition(Object left, Object right) {
            // Every record belongs to the one global partition.
        }
    };

    private SortPartitions() {
    }

    @SuppressWarnings("unchecked")
    public static <T> SortPartition<T> global() {
        return (SortPartition<T>) GLOBAL;
    }

    public static <T> SortPartition<T> byFields(String... fieldNames) {
        return byFieldsWithMessage("Sort can only move records within the same partition: " + String.join(", ", fieldNames),
                fieldNames);
    }

    public static <T> SortPartition<T> byFieldsWithMessage(String message, String... fieldNames) {
        List<String> fields = List.copyOf(Arrays.asList(fieldNames));
        return new SortPartition<>() {
            @Override
            public Criteria criteriaFor(T entity) {
                return BusinessScope.criteria(entity, fields.toArray(String[]::new));
            }

            @Override
            public void requireSamePartition(T left, T right) {
                BusinessScope.requireSame(left, right,
                        message,
                        fields.toArray(String[]::new));
            }
        };
    }

    public static <T> SortPartition<T> of(Function<T, Criteria> criteriaFactory, SortPartition<T> validation) {
        return new SortPartition<>() {
            @Override
            public Criteria criteriaFor(T entity) {
                return criteriaFactory.apply(entity);
            }

            @Override
            public void requireSamePartition(T left, T right) {
                validation.requireSamePartition(left, right);
            }
        };
    }

    public static <T> SortPartition<T> fromModel(Class<?> modelClass) {
        if (modelClass == null) {
            return global();
        }
        SortPartitionBy declaration = modelClass.getAnnotation(SortPartitionBy.class);
        if (declaration == null) {
            return global();
        }
        return declaration.message().isBlank()
                ? byFields(declaration.fields())
                : byFieldsWithMessage(declaration.message(), declaration.fields());
    }

    public static <T> SortPartition<T> compose(SortPartition<T> first, SortPartition<T> second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("sort partitions must not be null");
        }
        return new SortPartition<>() {
            @Override
            public Criteria criteriaFor(T entity) {
                Criteria criteria = first.criteriaFor(entity);
                Criteria additional = second.criteriaFor(entity);
                if (additional != null && !additional.isEmpty()) {
                    criteria.andGroup(additional.getRoot());
                }
                return criteria;
            }

            @Override
            public void requireSamePartition(T left, T right) {
                first.requireSamePartition(left, right);
                second.requireSamePartition(left, right);
            }
        };
    }
}
