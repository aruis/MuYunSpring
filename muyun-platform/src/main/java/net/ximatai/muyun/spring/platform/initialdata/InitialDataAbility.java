package net.ximatai.muyun.spring.platform.initialdata;

import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField;
import net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataRecord;

import java.util.Comparator;
import java.util.List;

public class InitialDataAbility {
    public static final String SYSTEM_OPERATOR_ID = "platform-initial-data";

    private final List<InitialDataContribution> contributions;

    public InitialDataAbility(List<InitialDataContribution> contributions) {
        this.contributions = contributions == null ? List.of() : List.copyOf(contributions);
    }

    public InitialDataExecutionReport initializeAll() {
        try (TenantContext.Scope ignored = TenantContext.system("initialize platform data");
             CurrentUserContext.Scope ignoredUser = CurrentUserContext.use(
                     CurrentUser.systemUser(SYSTEM_OPERATOR_ID, "Platform Initial Data"))) {
            List<InitialDataContributionReport> reports = contributions.stream()
                    .sorted(Comparator.comparingInt(InitialDataContribution::order)
                            .thenComparing(InitialDataContribution::name))
                    .map(this::execute)
                    .toList();
            return new InitialDataExecutionReport(reports);
        }
    }

    private InitialDataContributionReport execute(InitialDataContribution contribution) {
        List<InitialDataDeclaration<?>> declarations = contribution.declarations();
        if (declarations == null) {
            declarations = List.of();
        }
        List<InitialDataResult> results = declarations.stream()
                .map(declaration -> requireDeclaration(contribution, declaration))
                .map(this::apply)
                .toList();
        return new InitialDataContributionReport(contribution.name(), contribution.order(), results);
    }

    private InitialDataDeclaration<?> requireDeclaration(InitialDataContribution contribution,
                                                         InitialDataDeclaration<?> declaration) {
        if (declaration == null) {
            throw new IllegalArgumentException("Initial data declaration must not be null: " + contribution.name());
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
            declaration.insert();
            return new InitialDataResult(record.key(), record.policy(), InitialDataStatus.INSERTED, List.of());
        }
        rejectSoftDeleted(record, existing);
        rejectIdentityDrift(record, existing);
        if (record.policy() == InitialDataPolicy.ENSURE_ABSENT) {
            return new InitialDataResult(record.key(), record.policy(), InitialDataStatus.UNCHANGED, List.of());
        }

        List<String> changedFields = changedFields(record, existing);
        if (changedFields.isEmpty()) {
            return new InitialDataResult(record.key(), record.policy(), InitialDataStatus.UNCHANGED, List.of());
        }
        copyManagedFields(record, existing);
        declaration.update(existing);
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
}
