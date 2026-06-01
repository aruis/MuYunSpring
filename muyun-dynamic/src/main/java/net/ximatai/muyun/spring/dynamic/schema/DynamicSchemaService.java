package net.ximatai.muyun.spring.dynamic.schema;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.database.core.orm.MigrationResult;
import net.ximatai.muyun.database.core.orm.SchemaManager;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionValidator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public MigrationResult ensureTable(EntityDefinition entity, EntityDefinition previousEntity, MigrationOptions options) {
        return new SchemaManager(operations).ensureTable(tableMapper.toTable(entity, previousEntity), options);
    }

    public Map<String, Boolean> ensureModule(ModuleDefinition module) {
        Map<String, MigrationResult> migrations = ensureModule(module, MigrationOptions.execute());
        Map<String, Boolean> results = new LinkedHashMap<>();
        migrations.forEach((entityCode, migration) -> results.put(entityCode, migration.isChanged()));
        return results;
    }

    public Map<String, MigrationResult> ensureModule(ModuleDefinition module, MigrationOptions options) {
        return ensureModule(module, null, options);
    }

    public Map<String, MigrationResult> ensureModule(ModuleDefinition module,
                                                     ModuleDefinition previousModule,
                                                     MigrationOptions options) {
        validator.validate(module);
        if (previousModule != null) {
            validator.validate(previousModule);
        }
        Map<String, EntityDefinition> previousEntities = previousModule == null
                ? Map.of()
                : previousModule.entities().stream().collect(Collectors.toMap(EntityDefinition::code, Function.identity()));
        Map<String, MigrationResult> results = new LinkedHashMap<>();
        for (EntityDefinition entity : module.entities()) {
            results.put(entity.code(), ensureTable(entity, previousEntities.get(entity.code()), options));
        }
        return results;
    }
}
