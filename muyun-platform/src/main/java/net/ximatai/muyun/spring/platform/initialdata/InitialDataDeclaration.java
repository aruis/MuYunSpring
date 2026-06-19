package net.ximatai.muyun.spring.platform.initialdata;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataRecord;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Declarative initial data item executed by {@link InitialDataAbility}.
 * <p>
 * Application contributions should prefer domain-level declaration factories, such as menu declarations,
 * instead of assembling low-level SPI records and fields directly.
 */
public final class InitialDataDeclaration<T extends EntityContract> {
    private final InitialDataRecord<T> record;
    private final Supplier<T> existingReader;
    private final Consumer<T> inserter;
    private final Consumer<T> updater;

    private InitialDataDeclaration(InitialDataRecord<T> record,
                                   Supplier<T> existingReader,
                                   Consumer<T> inserter,
                                   Consumer<T> updater) {
        this.record = Objects.requireNonNull(record, "record must not be null");
        this.existingReader = Objects.requireNonNull(existingReader, "existingReader must not be null");
        this.inserter = Objects.requireNonNull(inserter, "inserter must not be null");
        this.updater = Objects.requireNonNull(updater, "updater must not be null");
    }

    public static <T extends EntityContract> InitialDataDeclaration<T> of(InitialDataRecord<T> record,
                                                                          Supplier<T> existingReader,
                                                                          Consumer<T> inserter,
                                                                          Consumer<T> updater) {
        return new InitialDataDeclaration<>(record, existingReader, inserter, updater);
    }

    public static <T extends EntityContract> InitialDataDeclaration<T> ensureAbsent(
            CrudAbility<T> service,
            T desired) {
        return fromService(service, InitialDataPolicy.ENSURE_ABSENT, desired);
    }

    public static <T extends EntityContract> InitialDataDeclaration<T> reconcileManaged(
            CrudAbility<T> service,
            T desired) {
        return fromService(service, InitialDataPolicy.RECONCILE_MANAGED, desired);
    }

    public static <T extends EntityContract> InitialDataDeclaration<T> locked(
            CrudAbility<T> service,
            T desired) {
        return fromService(service, InitialDataPolicy.LOCKED, desired);
    }

    private static <T extends EntityContract> InitialDataDeclaration<T> fromService(CrudAbility<T> service,
                                                                                   InitialDataPolicy policy,
                                                                                   T desired) {
        Objects.requireNonNull(service, "service must not be null");
        Objects.requireNonNull(desired, "desired must not be null");
        String id = requireText(desired.getId(), "initialDataId");
        @SuppressWarnings("unchecked")
        Class<T> modelClass = (Class<T>) Objects.requireNonNull(service.modelClass(),
                "service modelClass must not be null");
        InitialDataRecord<T> record = InitialDataModelDescriptor.of(modelClass).record(id, policy, desired);
        return of(record, () -> selectExisting(service, id), service::insert, service::update);
    }

    InitialDataRecord<T> record() {
        return record;
    }

    T existing() {
        return existingReader.get();
    }

    void insert() {
        inserter.accept(record.desired());
    }

    void update(T existing) {
        updater.accept(existing);
    }

    private static <T extends EntityContract> T selectExisting(CrudAbility<T> service, String id) {
        if (service instanceof SoftDeleteAbility<?> softDeleteAbility) {
            @SuppressWarnings("unchecked")
            SoftDeleteAbility<T> typed = (SoftDeleteAbility<T>) softDeleteAbility;
            return typed.selectIgnoreSoftDelete(id);
        }
        return service.select(id);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
