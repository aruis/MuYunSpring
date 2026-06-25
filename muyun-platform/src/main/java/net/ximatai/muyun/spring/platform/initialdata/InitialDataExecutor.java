package net.ximatai.muyun.spring.platform.initialdata;

import net.ximatai.muyun.spring.ability.initialdata.InitialDataAbility;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataOptions;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataPhase;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataPolicy;
import net.ximatai.muyun.spring.ability.PlatformManagedMutationContext;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField;
import net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class InitialDataExecutor {
    public static final String SYSTEM_OPERATOR_ID = "platform-initial-data";

    private final List<InitialDataAbility<?>> abilities;
    private final List<InitialDataDeclarationProvider> providers;

    public InitialDataExecutor(List<InitialDataAbility<?>> abilities, List<InitialDataDeclarationProvider> providers) {
        this.abilities = abilities == null ? List.of() : List.copyOf(abilities);
        this.providers = providers == null ? List.of() : List.copyOf(providers);
    }

    public InitialDataExecutionReport initializeAll() {
        try (TenantContext.Scope ignored = TenantContext.system("initialize platform data");
             CurrentUserContext.Scope ignoredUser = CurrentUserContext.use(
                     CurrentUser.systemUser(SYSTEM_OPERATOR_ID, "Platform Initial Data"))) {
            List<InitialDataTaskReport> reports = java.util.stream.Stream
                    .concat(abilityTasks().stream(), providerTasks().stream())
                    .sorted(Comparator.comparing(InitialDataTask::phase)
                            .thenComparingInt(InitialDataTask::order)
                            .thenComparing(InitialDataTask::name))
                    .map(this::execute)
                    .toList();
            return new InitialDataExecutionReport(reports);
        }
    }

    private List<InitialDataTask> abilityTasks() {
        return abilities.stream()
                .filter(Objects::nonNull)
                .map(this::abilityTask)
                .toList();
    }

    private InitialDataTask abilityTask(InitialDataAbility<?> ability) {
        InitialDataOptions options = ability.initialDataOptions();
        if (options == null) {
            options = InitialDataOptions.defaults();
        }
        final InitialDataOptions taskOptions = options;
        return new InitialDataTask(
                taskName(ability, taskOptions),
                taskOptions.phase(),
                taskOptions.order(),
                () -> abilityDeclarations(ability, taskOptions)
        );
    }

    private List<InitialDataDeclaration<?>> abilityDeclarations(InitialDataAbility<?> ability,
                                                                InitialDataOptions options) {
        List<?> initialData = ability.initialData();
        if (initialData == null || initialData.isEmpty()) {
            return List.of();
        }
        List<InitialDataDeclaration<?>> declarations = new ArrayList<>();
        for (Object item : initialData) {
            if (item != null) {
                declarations.add(declaration(ability, options, (EntityContract) item));
            }
        }
        return declarations;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private InitialDataDeclaration<?> declaration(InitialDataAbility<?> ability,
                                                  InitialDataOptions options,
                                                  EntityContract item) {
        InitialDataDeclaration<?> declaration = InitialDataDeclaration.fromService(
                (InitialDataAbility) ability,
                options.policy(),
                item
        );
        return options.tenantId() == null || options.tenantId().isBlank()
                ? declaration
                : declaration.inTenant(options.tenantId());
    }

    private String taskName(InitialDataAbility<?> ability, InitialDataOptions options) {
        if (options.name() != null && !options.name().isBlank()) {
            return options.name();
        }
        if (ability.getModuleAlias() != null && !ability.getModuleAlias().isBlank()) {
            return ability.getModuleAlias();
        }
        return ability.getClass().getName();
    }

    private List<InitialDataTask> providerTasks() {
        return providers.stream()
                .filter(Objects::nonNull)
                .map(provider -> new InitialDataTask(provider.name(), provider.phase(), provider.order(),
                        provider::declarations))
                .toList();
    }

    private InitialDataTaskReport execute(InitialDataTask task) {
        List<InitialDataDeclaration<?>> declarations = task.declarations();
        if (declarations == null) {
            declarations = List.of();
        }
        List<InitialDataResult> results = declarations.stream()
                .map(declaration -> requireDeclaration(task, declaration))
                .map(this::apply)
                .toList();
        return new InitialDataTaskReport(task.name(), task.order(), results);
    }

    private InitialDataDeclaration<?> requireDeclaration(InitialDataTask task,
                                                         InitialDataDeclaration<?> declaration) {
        if (declaration == null) {
            throw new IllegalArgumentException("Initial data declaration must not be null: " + task.name());
        }
        return declaration;
    }

    private InitialDataResult apply(InitialDataDeclaration<?> declaration) {
        return applyTyped(declaration);
    }

    private <T extends EntityContract> InitialDataResult applyTyped(InitialDataDeclaration<T> declaration) {
        InitialDataRecord<T> record = declaration.record();
        T existing = declaration.existing();
        if (existing == null) {
            PlatformManagedMutationContext.runAsPlatformManaged(declaration::insert);
            return new InitialDataResult(record.key(), record.policy(), InitialDataStatus.INSERTED, List.of());
        }
        rejectSoftDeleted(record, existing);
        rejectIdentityDrift(record, existing);
        if (record.policy() == InitialDataPolicy.CREATE_IF_MISSING) {
            return new InitialDataResult(record.key(), record.policy(), InitialDataStatus.UNCHANGED, List.of());
        }

        List<String> changedFields = changedFields(record, existing);
        if (changedFields.isEmpty()) {
            return new InitialDataResult(record.key(), record.policy(), InitialDataStatus.UNCHANGED, List.of());
        }
        copyManagedFields(record, existing);
        PlatformManagedMutationContext.runAsPlatformManaged(() -> declaration.update(existing));
        return new InitialDataResult(record.key(), record.policy(), InitialDataStatus.UPDATED, changedFields);
    }

    private void rejectSoftDeleted(InitialDataRecord<?> record, EntityContract existing) {
        if (Boolean.TRUE.equals(existing.getDeleted())) {
            throw new InitialDataConflictException("Initial data record is soft-deleted: " + record.key());
        }
    }

    private <T extends EntityContract> void rejectIdentityDrift(InitialDataRecord<T> record, T existing) {
        for (InitialDataField<T> field : record.identityFields()) {
            if (field.differs(existing, record.desired())) {
                throw new InitialDataConflictException("Initial data identity field drift: "
                        + record.key() + "." + field.name());
            }
        }
    }

    private <T extends EntityContract> List<String> changedFields(InitialDataRecord<T> record, T existing) {
        List<InitialDataField<T>> fields = record.policy() == InitialDataPolicy.LOCKED
                ? lockedFields(record)
                : record.managedFields();
        return fields.stream()
                .filter(field -> field.differs(existing, record.desired()))
                .map(InitialDataField::name)
                .toList();
    }

    private <T extends EntityContract> void copyManagedFields(InitialDataRecord<T> record, T existing) {
        List<InitialDataField<T>> fields = record.policy() == InitialDataPolicy.LOCKED
                ? lockedFields(record)
                : record.managedFields();
        for (InitialDataField<T> field : fields) {
            field.copy(existing, record.desired());
        }
    }

    private <T extends EntityContract> List<InitialDataField<T>> lockedFields(
            InitialDataRecord<T> record) {
        return java.util.stream.Stream.concat(record.managedFields().stream(), record.operatorFields().stream())
                .toList();
    }

    private record InitialDataTask(
            String name,
            InitialDataPhase phase,
            int order,
            java.util.function.Supplier<List<InitialDataDeclaration<?>>> declarationSupplier
    ) {
        List<InitialDataDeclaration<?>> declarations() {
            return declarationSupplier.get();
        }
    }
}
