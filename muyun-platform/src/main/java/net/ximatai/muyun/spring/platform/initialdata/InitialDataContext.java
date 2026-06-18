package net.ximatai.muyun.spring.platform.initialdata;

import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class InitialDataContext {
    private final List<InitialDataResult> results = new ArrayList<>();

    public <T extends EntityContract> InitialDataResult apply(InitialDataRecord<T> record,
                                                              Consumer<T> inserter,
                                                              Consumer<T> updater) {
        InitialDataResult result = doApply(record, inserter, updater);
        results.add(result);
        return result;
    }

    List<InitialDataResult> results() {
        return List.copyOf(results);
    }

    private <T extends EntityContract> InitialDataResult doApply(InitialDataRecord<T> record,
                                                                 Consumer<T> inserter,
                                                                 Consumer<T> updater) {
        if (record.existing() == null) {
            inserter.accept(record.desired());
            return new InitialDataResult(record.key(), record.policy(), InitialDataStatus.INSERTED, List.of());
        }
        rejectSoftDeleted(record);
        rejectIdentityDrift(record);
        if (record.policy() == InitialDataPolicy.ENSURE_ABSENT) {
            return new InitialDataResult(record.key(), record.policy(), InitialDataStatus.UNCHANGED, List.of());
        }

        List<String> changedFields = changedFields(record);
        if (changedFields.isEmpty()) {
            return new InitialDataResult(record.key(), record.policy(), InitialDataStatus.UNCHANGED, List.of());
        }
        copyManagedFields(record);
        updater.accept(record.existing());
        return new InitialDataResult(record.key(), record.policy(), InitialDataStatus.UPDATED, changedFields);
    }

    private <T extends EntityContract> void rejectSoftDeleted(InitialDataRecord<T> record) {
        if (Boolean.TRUE.equals(record.existing().getDeleted())) {
            throw new InitialDataConflictException("Initial data record is soft-deleted: " + record.key());
        }
    }

    private <T extends EntityContract> void rejectIdentityDrift(InitialDataRecord<T> record) {
        for (InitialDataField<T> field : record.identityFields()) {
            if (field.differs(record.existing(), record.desired())) {
                throw new InitialDataConflictException("Initial data identity field drift: "
                        + record.key() + "." + field.name());
            }
        }
    }

    private <T extends EntityContract> List<String> changedFields(InitialDataRecord<T> record) {
        List<InitialDataField<T>> fields = record.policy() == InitialDataPolicy.LOCKED
                ? lockedFields(record)
                : record.managedFields();
        return fields.stream()
                .filter(field -> field.differs(record.existing(), record.desired()))
                .map(InitialDataField::name)
                .toList();
    }

    private <T extends EntityContract> void copyManagedFields(InitialDataRecord<T> record) {
        List<InitialDataField<T>> fields = record.policy() == InitialDataPolicy.LOCKED
                ? lockedFields(record)
                : record.managedFields();
        for (InitialDataField<T> field : fields) {
            field.copy(record.existing(), record.desired());
        }
    }

    private <T extends EntityContract> List<InitialDataField<T>> lockedFields(InitialDataRecord<T> record) {
        List<InitialDataField<T>> fields = new ArrayList<>();
        fields.addAll(record.managedFields());
        fields.addAll(record.operatorFields());
        return fields;
    }
}
