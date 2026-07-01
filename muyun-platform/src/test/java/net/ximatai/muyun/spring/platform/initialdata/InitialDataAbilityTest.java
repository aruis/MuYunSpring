package net.ximatai.muyun.spring.platform.initialdata;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataAbility;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataOptions;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataPolicy;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.initialdata.InitialDataFields;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField;
import net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InitialDataAbilityTest {
    private static final InitialDataField<TestRecord> ID =
            InitialDataField.of("id", TestRecord::getId, TestRecord::setId);
    private static final InitialDataField<TestRecord> CODE =
            InitialDataField.of("code", TestRecord::getCode, TestRecord::setCode);
    private static final InitialDataField<TestRecord> MANAGED_VALUE =
            InitialDataField.of("managedValue", TestRecord::getManagedValue, TestRecord::setManagedValue);
    private static final InitialDataField<TestRecord> OPERATOR_TITLE =
            InitialDataField.of("operatorTitle", TestRecord::getOperatorTitle, TestRecord::setOperatorTitle);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        CurrentUserContext.clear();
    }

    @Test
    void shouldCreateIfMissingOnlyInsertMissingRecord() {
        TestRecord desired = record("demo", "code", "managed", "operator");
        AtomicInteger inserted = new AtomicInteger();
        AtomicInteger updated = new AtomicInteger();

        InitialDataResult result = apply(InitialDataRecord
                        .of("demo", InitialDataPolicy.CREATE_IF_MISSING, desired)
                        .identity(ID, CODE)
                        .managed(MANAGED_VALUE)
                        .operator(OPERATOR_TITLE),
                null,
                record -> inserted.incrementAndGet(),
                record -> updated.incrementAndGet());

        assertThat(result.status()).isEqualTo(InitialDataStatus.INSERTED);
        assertThat(inserted).hasValue(1);
        assertThat(updated).hasValue(0);

        TestRecord existing = record("demo", "code", "old-managed", "custom-operator");
        InitialDataResult unchanged = apply(InitialDataRecord
                        .of("demo", InitialDataPolicy.CREATE_IF_MISSING, desired)
                        .identity(ID, CODE)
                        .managed(MANAGED_VALUE)
                        .operator(OPERATOR_TITLE),
                existing,
                record -> inserted.incrementAndGet(),
                record -> updated.incrementAndGet());

        assertThat(unchanged.status()).isEqualTo(InitialDataStatus.UNCHANGED);
        assertThat(existing.getManagedValue()).isEqualTo("old-managed");
        assertThat(existing.getOperatorTitle()).isEqualTo("custom-operator");
        assertThat(inserted).hasValue(1);
        assertThat(updated).hasValue(0);
    }

    @Test
    void shouldReconcileManagedFieldsAndKeepOperatorFields() {
        TestRecord existing = record("demo", "code", "old-managed", "custom-operator");
        TestRecord desired = record("demo", "code", "new-managed", "default-operator");
        AtomicInteger updated = new AtomicInteger();

        InitialDataResult result = apply(InitialDataRecord
                        .of("demo", InitialDataPolicy.RECONCILE_MANAGED, desired)
                        .identity(ID, CODE)
                        .managed(MANAGED_VALUE)
                        .operator(OPERATOR_TITLE),
                existing,
                record -> {
                },
                record -> updated.incrementAndGet());

        assertThat(result.status()).isEqualTo(InitialDataStatus.UPDATED);
        assertThat(result.changedFields()).containsExactly("managedValue");
        assertThat(existing.getManagedValue()).isEqualTo("new-managed");
        assertThat(existing.getOperatorTitle()).isEqualTo("custom-operator");
        assertThat(updated).hasValue(1);
    }

    @Test
    void shouldLockedPolicyReconcileManagedAndOperatorFields() {
        TestRecord existing = record("demo", "code", "old-managed", "custom-operator");
        TestRecord desired = record("demo", "code", "new-managed", "default-operator");

        InitialDataResult result = apply(InitialDataRecord
                        .of("demo", InitialDataPolicy.LOCKED, desired)
                        .identity(ID, CODE)
                        .managed(MANAGED_VALUE)
                        .operator(OPERATOR_TITLE),
                existing,
                record -> {
                },
                record -> {
                });

        assertThat(result.status()).isEqualTo(InitialDataStatus.UPDATED);
        assertThat(result.changedFields()).containsExactly("managedValue", "operatorTitle");
        assertThat(existing.getManagedValue()).isEqualTo("new-managed");
        assertThat(existing.getOperatorTitle()).isEqualTo("default-operator");
    }

    @Test
    void shouldRejectIdentityDrift() {
        TestRecord existing = record("demo", "old-code", "managed", "operator");
        TestRecord desired = record("demo", "new-code", "managed", "operator");

        assertThatThrownBy(() -> apply(InitialDataRecord
                                .of("demo", InitialDataPolicy.RECONCILE_MANAGED, desired)
                                .identity(ID, CODE)
                                .managed(MANAGED_VALUE),
                        existing,
                        record -> {
                        },
                        record -> {
                        }))
                .isInstanceOf(InitialDataConflictException.class)
                .hasMessageContaining("demo.code");
    }

    @Test
    void shouldRejectIdentityDriftWhenCreatingIfMissing() {
        TestRecord existing = record("demo", "old-code", "managed", "operator");
        TestRecord desired = record("demo", "new-code", "managed", "operator");

        assertThatThrownBy(() -> apply(InitialDataRecord
                                .of("demo", InitialDataPolicy.CREATE_IF_MISSING, desired)
                                .identity(ID, CODE),
                        existing,
                        record -> {
                        },
                        record -> {
                        }))
                .isInstanceOf(InitialDataConflictException.class)
                .hasMessageContaining("demo.code");
    }

    @Test
    void shouldRejectSoftDeletedManagedRecord() {
        TestRecord existing = record("demo", "code", "managed", "operator");
        existing.setDeleted(Boolean.TRUE);
        TestRecord desired = record("demo", "code", "managed", "operator");

        assertThatThrownBy(() -> apply(InitialDataRecord
                                .of("demo", InitialDataPolicy.RECONCILE_MANAGED, desired)
                                .identity(ID, CODE),
                        existing,
                        record -> {
                        },
                        record -> {
                        }))
                .isInstanceOf(InitialDataConflictException.class)
                .hasMessageContaining("soft-deleted: demo");
    }

    @Test
    void shouldRunDeclarationProvidersInSystemContextByOrder() {
        List<String> executed = new ArrayList<>();
        InitialDataDeclarationProvider later = declarationProvider("later", 20, executed);
        InitialDataDeclarationProvider earlier = declarationProvider("earlier", 10, executed);
        InitialDataExecutor executor = new InitialDataExecutor(List.of(), List.of(later, earlier));

        InitialDataExecutionReport report = executor.initializeAll();

        assertThat(executed).containsExactly("earlier", "later");
        assertThat(report.tasks()).extracting(InitialDataTaskReport::name)
                .containsExactly("earlier", "later");
        assertThat(TenantContext.hasContext()).isFalse();
        assertThat(CurrentUserContext.currentUser()).isEmpty();
    }

    @Test
    void shouldReadExistingRecordWhenApplyingEachDeclaration() {
        Map<String, TestRecord> repository = new HashMap<>();
        TestRecord first = record("demo", "code", "first-managed", "operator");
        TestRecord second = record("demo", "code", "second-managed", "operator");
        InitialDataDeclarationProvider provider = () -> List.of(
                InitialDataDeclaration.of(recordDeclaration(first),
                        () -> repository.get(first.getId()),
                        record -> repository.put(record.getId(), record),
                        record -> repository.put(record.getId(), record)),
                InitialDataDeclaration.of(recordDeclaration(second),
                        () -> repository.get(second.getId()),
                        record -> repository.put(record.getId(), record),
                        record -> repository.put(record.getId(), record)));

        InitialDataExecutionReport report = new InitialDataExecutor(List.of(), List.of(provider)).initializeAll();

        assertThat(report.results()).extracting(InitialDataResult::status)
                .containsExactly(InitialDataStatus.INSERTED, InitialDataStatus.UPDATED);
        assertThat(repository.get("demo").getManagedValue()).isEqualTo("second-managed");
    }

    @Test
    void shouldRejectNullDeclarations() {
        InitialDataDeclarationProvider provider = () -> {
            List<InitialDataDeclaration<?>> declarations = new ArrayList<>();
            declarations.add(null);
            return declarations;
        };

        assertThatThrownBy(() -> new InitialDataExecutor(List.of(), List.of(provider)).initializeAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Initial data declaration must not be null");
    }

    @Test
    void shouldBuildDeclarationFromAnnotatedModel() {
        AnnotatedRecord existing = annotatedRecord("demo", "code", "old-managed", "custom-operator");
        AnnotatedRecord desired = annotatedRecord("demo", "code", "new-managed", "default-operator");
        TestCrudAbility<AnnotatedRecord> service = new TestCrudAbility<>(AnnotatedRecord.class, existing);
        InitialDataDeclarationProvider provider = () -> List.of(
                InitialDataDeclaration.reconcileManaged(service, desired));

        InitialDataResult result = new InitialDataExecutor(List.of(), List.of(provider)).initializeAll().results().getFirst();

        assertThat(result.status()).isEqualTo(InitialDataStatus.UPDATED);
        assertThat(result.changedFields()).containsExactly("managedValue");
        assertThat(existing.getManagedValue()).isEqualTo("new-managed");
        assertThat(existing.getOperatorTitle()).isEqualTo("custom-operator");
    }

    @Test
    void shouldBuildDeclarationWithCompositeIdentityAndSingleApplier() {
        CompositeAnnotatedRecord existing = compositeAnnotatedRecord("generated-id", "same-code", "old-managed",
                "custom-operator");
        CompositeAnnotatedRecord desired = compositeAnnotatedRecord(null, "same-code", "new-managed",
                "default-operator");
        AtomicInteger applied = new AtomicInteger();

        InitialDataDeclarationProvider provider = () -> List.of(
                InitialDataDeclaration.reconcileManaged(
                        "composite-key",
                        CompositeAnnotatedRecord.class,
                        desired,
                        () -> existing,
                        record -> applied.incrementAndGet()));

        InitialDataResult result = new InitialDataExecutor(List.of(), List.of(provider)).initializeAll().results().getFirst();

        assertThat(result.status()).isEqualTo(InitialDataStatus.UPDATED);
        assertThat(result.changedFields()).containsExactly("managedValue");
        assertThat(existing.getId()).isEqualTo("generated-id");
        assertThat(existing.getCode()).isEqualTo("same-code");
        assertThat(existing.getManagedValue()).isEqualTo("new-managed");
        assertThat(existing.getOperatorTitle()).isEqualTo("custom-operator");
        assertThat(applied).hasValue(1);
    }

    @Test
    void shouldBuildDeclarationsFromServiceAbility() {
        AnnotatedRecord existing = annotatedRecord("demo", "code", "old-managed", "custom-operator");
        AnnotatedRecord desired = annotatedRecord("demo", "code", "new-managed", "default-operator");
        TestInitialDataService service = new TestInitialDataService(existing, List.of(desired));

        InitialDataExecutionReport report = new InitialDataExecutor(
                List.<InitialDataAbility<?>>of(service), List.of()).initializeAll();

        assertThat(report.tasks()).extracting(InitialDataTaskReport::name)
                .containsExactly("test.initial_data");
        assertThat(report.results()).extracting(InitialDataResult::status)
                .containsExactly(InitialDataStatus.UPDATED);
        assertThat(existing.getManagedValue()).isEqualTo("new-managed");
        assertThat(existing.getOperatorTitle()).isEqualTo("custom-operator");
    }

    @Test
    void shouldFallbackToDesiredRecordClassWhenServiceModelClassIsUnavailable() {
        AnnotatedRecord desired = annotatedRecord("demo", "code", "managed", "operator");
        TestInitialDataService service = new TestInitialDataService(null, List.of(desired)) {
            @Override
            public Class<?> modelClass() {
                return null;
            }
        };

        InitialDataExecutionReport report = new InitialDataExecutor(
                List.<InitialDataAbility<?>>of(service), List.of()).initializeAll();

        assertThat(report.results()).extracting(InitialDataResult::status)
                .containsExactly(InitialDataStatus.INSERTED);
        assertThat(service.select("demo").getCode()).isEqualTo("code");
    }

    private InitialDataDeclarationProvider declarationProvider(String name, int order, List<String> executed) {
        return new InitialDataDeclarationProvider() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public int order() {
                return order;
            }

            @Override
            public List<InitialDataDeclaration<?>> declarations() {
                assertThat(TenantContext.isSystem()).isTrue();
                assertThat(TenantContext.systemReason()).contains("initialize platform data");
                assertThat(CurrentUserContext.currentUser())
                        .hasValueSatisfying(user -> assertThat(user.userId())
                                .isEqualTo(InitialDataExecutor.SYSTEM_OPERATOR_ID));
                executed.add(name);
                return List.of();
            }
        };
    }

    private InitialDataResult apply(InitialDataRecord<TestRecord> record,
                                    TestRecord existing,
                                    java.util.function.Consumer<TestRecord> inserter,
                                    java.util.function.Consumer<TestRecord> updater) {
        InitialDataDeclarationProvider provider = () -> List.of(InitialDataDeclaration.of(record, () -> existing,
                inserter, updater));
        return new InitialDataExecutor(List.of(), List.of(provider)).initializeAll().results().getFirst();
    }

    private InitialDataRecord<TestRecord> recordDeclaration(TestRecord desired) {
        return InitialDataRecord
                .of(desired.getId(), InitialDataPolicy.RECONCILE_MANAGED, desired)
                .identity(ID, CODE)
                .managed(MANAGED_VALUE)
                .operator(OPERATOR_TITLE);
    }

    private static TestRecord record(String id, String code, String managedValue, String operatorTitle) {
        TestRecord record = new TestRecord();
        record.setId(id);
        record.setCode(code);
        record.setManagedValue(managedValue);
        record.setOperatorTitle(operatorTitle);
        return record;
    }

    private static class TestRecord extends StandardEntity {
        private String code;
        private String managedValue;
        private String operatorTitle;

        String getCode() {
            return code;
        }

        void setCode(String code) {
            this.code = code;
        }

        String getManagedValue() {
            return managedValue;
        }

        void setManagedValue(String managedValue) {
            this.managedValue = managedValue;
        }

        String getOperatorTitle() {
            return operatorTitle;
        }

        void setOperatorTitle(String operatorTitle) {
            this.operatorTitle = operatorTitle;
        }
    }

    @InitialDataFields(
            identity = "code",
            managed = "managedValue",
            operator = "operatorTitle"
    )
    private static class AnnotatedRecord extends StandardEntity {
        private String code;
        private String managedValue;
        private String operatorTitle;

        String getCode() {
            return code;
        }

        void setCode(String code) {
            this.code = code;
        }

        String getManagedValue() {
            return managedValue;
        }

        void setManagedValue(String managedValue) {
            this.managedValue = managedValue;
        }

        String getOperatorTitle() {
            return operatorTitle;
        }

        void setOperatorTitle(String operatorTitle) {
            this.operatorTitle = operatorTitle;
        }
    }

    private static AnnotatedRecord annotatedRecord(String id, String code, String managedValue, String operatorTitle) {
        AnnotatedRecord record = new AnnotatedRecord();
        record.setId(id);
        record.setCode(code);
        record.setManagedValue(managedValue);
        record.setOperatorTitle(operatorTitle);
        return record;
    }

    @InitialDataFields(
            includeId = false,
            identity = "code",
            managed = "managedValue",
            operator = "operatorTitle"
    )
    private static class CompositeAnnotatedRecord extends AnnotatedRecord {
    }

    private static CompositeAnnotatedRecord compositeAnnotatedRecord(String id,
                                                                     String code,
                                                                     String managedValue,
                                                                     String operatorTitle) {
        CompositeAnnotatedRecord record = new CompositeAnnotatedRecord();
        record.setId(id);
        record.setCode(code);
        record.setManagedValue(managedValue);
        record.setOperatorTitle(operatorTitle);
        return record;
    }

    private static class TestCrudAbility<T extends StandardEntity> implements CrudAbility<T> {
        private final Class<T> modelClass;
        private T existing;

        TestCrudAbility(Class<T> modelClass, T existing) {
            this.modelClass = modelClass;
            this.existing = existing;
        }

        @Override
        public BaseDao<T, String> getDao() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getModuleAlias() {
            return "test.initial_data";
        }

        @Override
        public Class<?> modelClass() {
            return modelClass;
        }

        @Override
        public T select(String id) {
            return existing;
        }

        @Override
        public String insert(T entity) {
            existing = entity;
            return entity.getId();
        }

        @Override
        public int update(T entity) {
            existing = entity;
            return 1;
        }
    }

    private static class TestInitialDataService extends TestCrudAbility<AnnotatedRecord>
            implements InitialDataAbility<AnnotatedRecord> {
        private final List<AnnotatedRecord> initialData;

        TestInitialDataService(AnnotatedRecord existing, List<AnnotatedRecord> initialData) {
            super(AnnotatedRecord.class, existing);
            this.initialData = initialData;
        }

        @Override
        public InitialDataOptions initialDataOptions() {
            return InitialDataOptions.defaults().order(10);
        }

        @Override
        public List<AnnotatedRecord> initialData() {
            return initialData;
        }
    }
}
