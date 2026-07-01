package net.ximatai.muyun.spring.platform.initialdata;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataAbility;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataPolicy;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataRecord;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Declarative initial data item executed by {@link InitialDataExecutor}.
 * <p>
 * Regular service-owned initial data should implement {@link InitialDataAbility} and let the executor build
 * declarations from service and model contracts. This type remains available for platform providers that
 * need composite identity or scanned declarations.
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

    public static <T extends EntityContract> InitialDataDeclaration<T> createIfMissing(
            CrudAbility<T> service,
            T desired) {
        return fromService(service, InitialDataPolicy.CREATE_IF_MISSING, desired);
    }

    public static <T extends EntityContract> InitialDataDeclaration<T> createIfMissing(
            Class<T> modelClass,
            T desired,
            Supplier<T> existingReader,
            Consumer<T> applier) {
        return fromFunctions(modelClass, InitialDataPolicy.CREATE_IF_MISSING, desired, existingReader, applier,
                applier);
    }

    public static <T extends EntityContract> InitialDataDeclaration<T> createIfMissing(
            String key,
            Class<T> modelClass,
            T desired,
            Supplier<T> existingReader,
            Consumer<T> applier) {
        return fromFunctions(key, modelClass, InitialDataPolicy.CREATE_IF_MISSING, desired, existingReader, applier,
                applier);
    }

    public static <T extends EntityContract> InitialDataDeclaration<T> createIfMissing(
            Class<T> modelClass,
            T desired,
            Supplier<T> existingReader,
            Consumer<T> inserter,
            Consumer<T> updater) {
        return fromFunctions(modelClass, InitialDataPolicy.CREATE_IF_MISSING, desired, existingReader, inserter,
                updater);
    }

    public static <T extends EntityContract> InitialDataDeclaration<T> reconcileManaged(
            CrudAbility<T> service,
            T desired) {
        return fromService(service, InitialDataPolicy.RECONCILE_MANAGED, desired);
    }

    public static <T extends EntityContract> InitialDataDeclaration<T> reconcileManaged(
            Class<T> modelClass,
            T desired,
            Supplier<T> existingReader,
            Consumer<T> applier) {
        return fromFunctions(modelClass, InitialDataPolicy.RECONCILE_MANAGED, desired, existingReader, applier, applier);
    }

    public static <T extends EntityContract> InitialDataDeclaration<T> reconcileManaged(
            String key,
            Class<T> modelClass,
            T desired,
            Supplier<T> existingReader,
            Consumer<T> applier) {
        return fromFunctions(key, modelClass, InitialDataPolicy.RECONCILE_MANAGED, desired, existingReader, applier,
                applier);
    }

    public static <T extends EntityContract> InitialDataDeclaration<T> reconcileManaged(
            Class<T> modelClass,
            T desired,
            Supplier<T> existingReader,
            Consumer<T> inserter,
            Consumer<T> updater) {
        return fromFunctions(modelClass, InitialDataPolicy.RECONCILE_MANAGED, desired, existingReader, inserter, updater);
    }

    public static <T extends EntityContract> InitialDataDeclaration<T> reconcileManaged(
            String key,
            Class<T> modelClass,
            T desired,
            Supplier<T> existingReader,
            Consumer<T> inserter,
            Consumer<T> updater) {
        return fromFunctions(key, modelClass, InitialDataPolicy.RECONCILE_MANAGED, desired, existingReader, inserter,
                updater);
    }

    public static <T extends EntityContract> InitialDataDeclaration<T> locked(
            CrudAbility<T> service,
            T desired) {
        return fromService(service, InitialDataPolicy.LOCKED, desired);
    }

    public static <T extends EntityContract> InitialDataDeclaration<T> fromService(CrudAbility<T> service,
                                                                                   InitialDataPolicy policy,
                                                                                   T desired) {
        Objects.requireNonNull(service, "service must not be null");
        Objects.requireNonNull(desired, "desired must not be null");
        String id = requireText(desired.getId(), "initialDataId");
        @SuppressWarnings("unchecked")
        Class<T> modelClass = (Class<T>) (service.modelClass() == null ? desired.getClass() : service.modelClass());
        InitialDataRecord<T> record = InitialDataModelDescriptor.of(modelClass).record(id, policy, desired);
        return of(record, () -> selectExisting(service, id), service::insert, service::update);
    }

    public static <T extends EntityContract> InitialDataDeclaration<T> locked(
            Class<T> modelClass,
            T desired,
            Supplier<T> existingReader,
            Consumer<T> applier) {
        return fromFunctions(modelClass, InitialDataPolicy.LOCKED, desired, existingReader, applier, applier);
    }

    public static <T extends EntityContract> InitialDataDeclaration<T> locked(
            String key,
            Class<T> modelClass,
            T desired,
            Supplier<T> existingReader,
            Consumer<T> applier) {
        return fromFunctions(key, modelClass, InitialDataPolicy.LOCKED, desired, existingReader, applier, applier);
    }

    public static <T extends EntityContract> InitialDataDeclaration<T> locked(
            Class<T> modelClass,
            T desired,
            Supplier<T> existingReader,
            Consumer<T> inserter,
            Consumer<T> updater) {
        return fromFunctions(modelClass, InitialDataPolicy.LOCKED, desired, existingReader, inserter, updater);
    }

    public InitialDataDeclaration<T> inTenant(String tenantId) {
        String validTenantId = requireText(tenantId, "tenantId");
        return new InitialDataDeclaration<>(
                record,
                () -> inTenant(validTenantId, existingReader),
                entity -> inTenant(validTenantId, () -> {
                    inserter.accept(entity);
                    return null;
                }),
                entity -> inTenant(validTenantId, () -> {
                    updater.accept(entity);
                    return null;
                })
        );
    }

    private static <T extends EntityContract> InitialDataDeclaration<T> fromFunctions(
            Class<T> modelClass,
            InitialDataPolicy policy,
            T desired,
            Supplier<T> existingReader,
            Consumer<T> inserter,
            Consumer<T> updater) {
        Objects.requireNonNull(modelClass, "modelClass must not be null");
        Objects.requireNonNull(desired, "desired must not be null");
        String id = requireText(desired.getId(), "initialDataId");
        return fromFunctions(id, modelClass, policy, desired, existingReader, inserter, updater);
    }

    private static <T extends EntityContract> InitialDataDeclaration<T> fromFunctions(
            String key,
            Class<T> modelClass,
            InitialDataPolicy policy,
            T desired,
            Supplier<T> existingReader,
            Consumer<T> inserter,
            Consumer<T> updater) {
        Objects.requireNonNull(modelClass, "modelClass must not be null");
        Objects.requireNonNull(desired, "desired must not be null");
        InitialDataRecord<T> record = InitialDataModelDescriptor.of(modelClass)
                .record(requireText(key, "initialDataKey"), policy, desired);
        return of(record, existingReader, inserter, updater);
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

    private static <R> R inTenant(String tenantId, Supplier<R> action) {
        try (TenantContext.Scope ignored = TenantContext.use(tenantId)) {
            return action.get();
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
