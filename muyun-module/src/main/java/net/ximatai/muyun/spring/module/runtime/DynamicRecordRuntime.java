package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;

import java.util.Objects;

public class DynamicRecordRuntime {
    private final IDatabaseOperations<?> operations;

    public DynamicRecordRuntime(IDatabaseOperations<?> operations) {
        this.operations = Objects.requireNonNull(operations, "operations must not be null");
    }

    public DynamicRecordAbility ability(String moduleAlias, EntityDefinition entity) {
        return ability(moduleAlias, entity, DynamicRecordLifecycle.NONE);
    }

    public DynamicRecordAbility ability(String moduleAlias, EntityDefinition entity, DynamicRecordLifecycle lifecycle) {
        return new DynamicRecordAbility(new DynamicRecordDao(operations, entity), moduleAlias, lifecycle);
    }
}
