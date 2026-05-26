package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.database.core.orm.MigrationResult;
import net.ximatai.muyun.database.core.orm.SchemaManager;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinitionValidator;

import java.util.LinkedHashMap;
import java.util.Map;

public class DynamicSchemaService {
    private final IDatabaseOperations<?> operations;
    private final DynamicTableMapper tableMapper;
    private final ModuleDefinitionValidator validator;

    public DynamicSchemaService(IDatabaseOperations<?> operations) {
        this(operations, new DynamicTableMapper(), new ModuleDefinitionValidator());
    }

    public DynamicSchemaService(IDatabaseOperations<?> operations,
                                DynamicTableMapper tableMapper,
                                ModuleDefinitionValidator validator) {
        this.operations = operations;
        this.tableMapper = tableMapper;
        this.validator = validator;
    }

    public boolean ensureTable(EntityDefinition entity) {
        return ensureTable(entity, MigrationOptions.execute()).isChanged();
    }

    public MigrationResult ensureTable(EntityDefinition entity, MigrationOptions options) {
        return new SchemaManager(operations).ensureTable(tableMapper.toTable(entity), options);
    }

    public Map<String, Boolean> ensureModule(ModuleDefinition module) {
        validator.validate(module);
        Map<String, Boolean> results = new LinkedHashMap<>();
        for (EntityDefinition entity : module.entities()) {
            results.put(entity.code(), ensureTable(entity));
        }
        return results;
    }
}
