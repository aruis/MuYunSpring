package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.database.core.orm.MigrationResult;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinitionException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicModulePublisherTest {
    @Test
    void shouldEnsureSchemaThenRegisterRuntimeModule() {
        RecordingSchemaService schemaService = new RecordingSchemaService(false);
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations());
        DynamicModulePublisher publisher = new DynamicModulePublisher(schemaService, runtime);

        DynamicModulePublishResult result = publisher.publish(contractModule());

        assertThat(result.changed()).isTrue();
        assertThat(result.migrations()).containsKey("contract");
        assertThat(schemaService.ensuredEntities).containsExactly("contract");
        assertThat(runtime.registry().requireEntity("sales.contract", "contract").tableName())
                .isEqualTo("app_contract");
    }

    @Test
    void shouldNotRegisterDryRunPublication() {
        RecordingSchemaService schemaService = new RecordingSchemaService(true);
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations());
        DynamicModulePublisher publisher = new DynamicModulePublisher(schemaService, runtime);

        DynamicModulePublishResult result = publisher.publish(contractModule(), MigrationOptions.dryRun());

        assertThat(result.migrations().get("contract").isDryRun()).isTrue();
        assertThatThrownBy(() -> runtime.entityService("sales.contract", "contract"))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("unknown module alias");
    }

    @Test
    void shouldKeepExistingRuntimeDefinitionWhenDryRunPublishingEvolution() {
        RecordingSchemaService schemaService = new RecordingSchemaService(true);
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations())
                .register(contractModule());
        DynamicModulePublisher publisher = new DynamicModulePublisher(schemaService, runtime);

        DynamicModulePublishResult result = publisher.publish(evolvedContractModule(), MigrationOptions.dryRun());

        assertThat(result.migrations().get("contract").isDryRun()).isTrue();
        assertThat(schemaService.previousModules.get("sales.contract")).isEqualTo(contractModule());
        assertThat(runtime.registry().requireEntity("sales.contract", "contract").fields())
                .extracting(FieldDefinition::fieldName)
                .containsExactly("code", "amount");
    }

    @Test
    void shouldReplaceRuntimeDefinitionAfterSchemaEvolutionSucceeds() {
        RecordingSchemaService schemaService = new RecordingSchemaService(false);
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations())
                .register(contractModule());
        DynamicModulePublisher publisher = new DynamicModulePublisher(schemaService, runtime);

        publisher.publish(evolvedContractModule());

        assertThat(schemaService.ensuredEntities).containsExactly("contract");
        assertThat(schemaService.previousModules.get("sales.contract")).isEqualTo(contractModule());
        assertThat(runtime.registry().requireEntity("sales.contract", "contract").fields())
                .extracting(FieldDefinition::fieldName)
                .containsExactly("code", "amount", "title");
    }

    @Test
    void shouldKeepExistingRuntimeDefinitionWhenSchemaEvolutionFails() {
        FailingSchemaService schemaService = new FailingSchemaService();
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations())
                .register(contractModule());
        DynamicModulePublisher publisher = new DynamicModulePublisher(schemaService, runtime);

        assertThatThrownBy(() -> publisher.publish(evolvedContractModule()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("schema failed");
        assertThat(runtime.registry().requireEntity("sales.contract", "contract").fields())
                .extracting(FieldDefinition::fieldName)
                .containsExactly("code", "amount");
    }

    @Test
    void shouldValidateBeforeRegisteringModule() {
        RecordingSchemaService schemaService = new RecordingSchemaService(false);
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations());
        DynamicModulePublisher publisher = new DynamicModulePublisher(schemaService, runtime);
        ModuleDefinition invalid = new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(
                        entity("contract", "app_contract"),
                        entity("contract_copy", "app_contract")
                )
        );

        assertThatThrownBy(() -> publisher.publish(invalid))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("duplicate table name");
        assertThat(schemaService.ensuredEntities).isEmpty();
        assertThat(runtime.registry().findModule("sales.contract")).isEmpty();
    }

    @Test
    void shouldRegisterFirstPublicationThroughPublisher() {
        RecordingSchemaService schemaService = new RecordingSchemaService(false);
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations());
        DynamicModulePublisher publisher = new DynamicModulePublisher(schemaService, runtime);

        publisher.publish(contractModule());

        assertThat(runtime.registry().findModule("sales.contract")).isPresent();
        assertThat(schemaService.ensuredEntities).containsExactly("contract");
    }

    @SuppressWarnings("unchecked")
    private IDatabaseOperations<Object> operations() {
        IDatabaseOperations<Object> operations = mock(IDatabaseOperations.class);
        when(operations.getDBInfo()).thenReturn(new DBInfo("POSTGRESQL").setName("muyun_test"));
        when(operations.getDefaultSchemaName()).thenReturn("public");
        return operations;
    }

    private ModuleDefinition contractModule() {
        return new ModuleDefinition("sales.contract", "Contract", List.of(entity("contract", "app_contract")));
    }

    private ModuleDefinition evolvedContractModule() {
        return new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(new EntityDefinition(
                        "contract",
                        "app_contract",
                        "Contract",
                        List.of(
                                FieldDefinition.string("code", "Code").length(64).required(),
                                FieldDefinition.decimal("amount", "Amount").precision(18, 2),
                                FieldDefinition.titleField()
                        )
                ).withCapabilities(net.ximatai.muyun.spring.module.metadata.EntityCapability.CRUD,
                        net.ximatai.muyun.spring.module.metadata.EntityCapability.REFERENCE))
        );
    }

    private EntityDefinition entity(String code, String tableName) {
        return new EntityDefinition(
                code,
                tableName,
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required(),
                        FieldDefinition.decimal("amount", "Amount").precision(18, 2)
                )
        );
    }

    private static class RecordingSchemaService extends DynamicSchemaService {
        private final boolean dryRun;
        private final List<String> ensuredEntities = new java.util.ArrayList<>();
        private final Map<String, ModuleDefinition> previousModules = new LinkedHashMap<>();

        RecordingSchemaService(boolean dryRun) {
            super(null);
            this.dryRun = dryRun;
        }

        @Override
        public Map<String, MigrationResult> ensureModule(ModuleDefinition module,
                                                         ModuleDefinition previousModule,
                                                         MigrationOptions options) {
            new net.ximatai.muyun.spring.module.metadata.ModuleDefinitionValidator().validate(module);
            if (previousModule != null) {
                previousModules.put(module.moduleAlias(), previousModule);
            }
            Map<String, MigrationResult> results = new LinkedHashMap<>();
            for (EntityDefinition entity : module.entities()) {
                ensuredEntities.add(entity.code());
                results.put(entity.code(), new MigrationResult(true, dryRun, false, List.of()));
            }
            return results;
        }
    }

    private static class FailingSchemaService extends DynamicSchemaService {
        FailingSchemaService() {
            super(null);
        }

        @Override
        public Map<String, MigrationResult> ensureModule(ModuleDefinition module,
                                                         ModuleDefinition previousModule,
                                                         MigrationOptions options) {
            throw new IllegalStateException("schema failed");
        }
    }
}
