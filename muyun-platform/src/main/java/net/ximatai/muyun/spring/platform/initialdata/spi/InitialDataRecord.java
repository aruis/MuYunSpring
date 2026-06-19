package net.ximatai.muyun.spring.platform.initialdata.spi;

import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataPolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Low-level record template for building domain-specific initial data declaration factories.
 * Regular business contributions should not depend on this type directly.
 */
public final class InitialDataRecord<T extends EntityContract> {
    private final String key;
    private final InitialDataPolicy policy;
    private final T desired;
    private final List<InitialDataField<T>> identityFields = new ArrayList<>();
    private final List<InitialDataField<T>> managedFields = new ArrayList<>();
    private final List<InitialDataField<T>> operatorFields = new ArrayList<>();

    private InitialDataRecord(String key, InitialDataPolicy policy, T desired) {
        this.key = requireText(key, "initialDataKey");
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
        this.desired = Objects.requireNonNull(desired, "desired must not be null");
    }

    public static <T extends EntityContract> InitialDataRecord<T> of(String key, InitialDataPolicy policy, T desired) {
        return new InitialDataRecord<>(key, policy, desired);
    }

    @SafeVarargs
    public final InitialDataRecord<T> identity(InitialDataField<T>... fields) {
        add(identityFields, fields);
        return this;
    }

    @SafeVarargs
    public final InitialDataRecord<T> managed(InitialDataField<T>... fields) {
        add(managedFields, fields);
        return this;
    }

    @SafeVarargs
    public final InitialDataRecord<T> operator(InitialDataField<T>... fields) {
        add(operatorFields, fields);
        return this;
    }

    public String key() {
        return key;
    }

    public InitialDataPolicy policy() {
        return policy;
    }

    public T desired() {
        return desired;
    }

    public List<InitialDataField<T>> identityFields() {
        return List.copyOf(identityFields);
    }

    public List<InitialDataField<T>> managedFields() {
        return List.copyOf(managedFields);
    }

    public List<InitialDataField<T>> operatorFields() {
        return List.copyOf(operatorFields);
    }

    private void add(List<InitialDataField<T>> target, InitialDataField<T>[] fields) {
        if (fields == null || fields.length == 0) {
            return;
        }
        Arrays.stream(fields)
                .filter(Objects::nonNull)
                .forEach(target::add);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
