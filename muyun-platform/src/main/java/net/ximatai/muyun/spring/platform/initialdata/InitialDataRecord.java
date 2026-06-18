package net.ximatai.muyun.spring.platform.initialdata;

import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class InitialDataRecord<T extends EntityContract> {
    private final String key;
    private final InitialDataPolicy policy;
    private final T existing;
    private final T desired;
    private final List<InitialDataField<T>> identityFields = new ArrayList<>();
    private final List<InitialDataField<T>> managedFields = new ArrayList<>();
    private final List<InitialDataField<T>> operatorFields = new ArrayList<>();

    private InitialDataRecord(String key, InitialDataPolicy policy, T existing, T desired) {
        this.key = requireText(key, "initialDataKey");
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
        this.existing = existing;
        this.desired = Objects.requireNonNull(desired, "desired must not be null");
    }

    public static <T extends EntityContract> InitialDataRecord<T> of(String key, InitialDataPolicy policy, T existing, T desired) {
        return new InitialDataRecord<>(key, policy, existing, desired);
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

    public T existing() {
        return existing;
    }

    public T desired() {
        return desired;
    }

    List<InitialDataField<T>> identityFields() {
        return List.copyOf(identityFields);
    }

    List<InitialDataField<T>> managedFields() {
        return List.copyOf(managedFields);
    }

    List<InitialDataField<T>> operatorFields() {
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
