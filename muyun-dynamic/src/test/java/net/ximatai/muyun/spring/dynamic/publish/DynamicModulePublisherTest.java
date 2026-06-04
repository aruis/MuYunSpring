package net.ximatai.muyun.spring.dynamic.publish;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.database.core.orm.MigrationResult;
import net.ximatai.muyun.spring.ability.event.RuntimeEvent;
import net.ximatai.muyun.spring.ability.event.RuntimeEventPublisher;
import net.ximatai.muyun.spring.ability.event.RuntimeEventType;
import net.ximatai.muyun.spring.ability.event.RuntimeMutationSource;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicFieldValueValidator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicModuleRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import net.ximatai.muyun.spring.dynamic.schema.DynamicSchemaService;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.ArrayList;
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
    void shouldPublishModuleEventAfterRuntimePublication() {
        RecordingSchemaService schemaService = new RecordingSchemaService(false);
        CollectingRuntimeEventPublisher events = new CollectingRuntimeEventPublisher();
        DynamicRecordRuntime runtime = runtime(events);
        DynamicModulePublisher publisher = new DynamicModulePublisher(schemaService, runtime);

        publisher.publish(contractModule());

        assertThat(events.events()).hasSize(1);
        RuntimeEvent event = events.events().getFirst();
        assertThat(event.eventType()).isEqualTo(RuntimeEventType.MODULE_PUBLISHED);
        assertThat(event.moduleAlias()).isEqualTo("sales.contract");
        assertThat(event.entityAlias()).isNull();
        assertThat(event.mutationSource()).isEqualTo(RuntimeMutationSource.SYSTEM);
        assertThat(event.payload()).containsEntry("changed", Boolean.TRUE)
                .containsEntry("nonAdditiveChanges", Boolean.FALSE);
        assertThat(event.payload().get("entities")).isEqualTo(List.of(Map.of(
                "entityAlias", "contract",
                "changed", Boolean.TRUE,
                "dryRun", Boolean.FALSE,
                "nonAdditiveChanges", Boolean.FALSE,
                "statements", List.of()
        )));
    }

    @Test
    void shouldNotPublishModuleEventForPreview() {
        RecordingSchemaService schemaService = new RecordingSchemaService(true);
        CollectingRuntimeEventPublisher events = new CollectingRuntimeEventPublisher();
        DynamicModulePublisher publisher = new DynamicModulePublisher(schemaService, runtime(events));

        publisher.preview(contractModule());

        assertThat(events.events()).isEmpty();
    }

    @Test
    void shouldKeepSystemContextOnModulePublicationEvent() {
        RecordingSchemaService schemaService = new RecordingSchemaService(false);
        CollectingRuntimeEventPublisher events = new CollectingRuntimeEventPublisher();
        DynamicModulePublisher publisher = new DynamicModulePublisher(schemaService, runtime(events));

        try (TenantContext.Scope ignored = TenantContext.system("test system context")) {
            publisher.publish(contractModule());
        }

        assertThat(events.events()).singleElement()
                .satisfies(event -> assertThat(event.systemContext()).isTrue());
    }

    @Test
    void shouldNotRegisterDryRunPublication() {
        RecordingSchemaService schemaService = new RecordingSchemaService(true);
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations());
        DynamicModulePublisher publisher = new DynamicModulePublisher(schemaService, runtime);

        DynamicModulePublishResult result = publisher.preview(contractModule());

        assertThat(result.dryRun()).isTrue();
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

        DynamicModulePublishResult result = publisher.preview(evolvedContractModule());

        assertThat(result.dryRun()).isTrue();
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
    void shouldExposePublishResultSummaryWithoutTraversingMigrations() {
        RecordingSchemaService schemaService = new RecordingSchemaService(true, true);
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations());
        DynamicModulePublisher publisher = new DynamicModulePublisher(schemaService, runtime);

        DynamicModulePublishResult result = publisher.preview(contractModule());

        assertThat(result.changed()).isTrue();
        assertThat(result.dryRun()).isTrue();
        assertThat(result.hasNonAdditiveChanges()).isTrue();
        assertThat(result.statementsByEntity()).containsEntry("contract", List.of("alter table app_contract drop column name"));
    }

    @Test
    void shouldKeepPreviewSideEffectFreeEvenWhenSchemaReturnsNoMigrations() {
        EmptySchemaService schemaService = new EmptySchemaService();
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations());
        DynamicModulePublisher publisher = new DynamicModulePublisher(schemaService, runtime);

        DynamicModulePublishResult result = publisher.preview(emptyModule());

        assertThat(result.dryRun()).isTrue();
        assertThat(result.changed()).isFalse();
        assertThat(schemaService.lastOptions.isDryRun()).isTrue();
        assertThat(runtime.registry().findModule("sales.empty")).isEmpty();
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

    private DynamicRecordRuntime runtime(RuntimeEventPublisher eventPublisher) {
        return new DynamicRecordRuntime(operations(), new DynamicModuleRegistry(),
                DynamicFieldValueValidator.NONE, eventPublisher);
    }

    private ModuleDefinition emptyModule() {
        return new ModuleDefinition("sales.empty", "Empty", List.of());
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
                ).withCapabilities(net.ximatai.muyun.spring.common.platform.EntityCapability.CRUD,
                        net.ximatai.muyun.spring.common.platform.EntityCapability.REFERENCE))
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
        private MigrationOptions lastOptions;

        RecordingSchemaService(boolean dryRun) {
            this(dryRun, false);
        }

        RecordingSchemaService(boolean dryRun, boolean nonAdditive) {
            super(null);
            this.dryRun = dryRun;
            this.nonAdditive = nonAdditive;
        }

        private final boolean nonAdditive;

        @Override
        public Map<String, MigrationResult> ensureModule(ModuleDefinition module,
                                                         ModuleDefinition previousModule,
                                                         MigrationOptions options) {
            new net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionValidator().validate(module);
            lastOptions = options;
            if (previousModule != null) {
                previousModules.put(module.moduleAlias(), previousModule);
            }
            Map<String, MigrationResult> results = new LinkedHashMap<>();
            for (EntityDefinition entity : module.entities()) {
                ensuredEntities.add(entity.alias());
                results.put(entity.alias(), new MigrationResult(
                        true,
                        dryRun,
                        nonAdditive,
                        nonAdditive ? List.of("alter table app_contract drop column name") : List.of()
                ));
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

    private static class EmptySchemaService extends DynamicSchemaService {
        private MigrationOptions lastOptions;

        EmptySchemaService() {
            super(null);
        }

        @Override
        public Map<String, MigrationResult> ensureModule(ModuleDefinition module,
                                                         ModuleDefinition previousModule,
                                                         MigrationOptions options) {
            lastOptions = options;
            return Map.of();
        }
    }

    private static final class CollectingRuntimeEventPublisher implements RuntimeEventPublisher {
        private final List<RuntimeEvent> events = new ArrayList<>();

        @Override
        public void publish(RuntimeEvent event) {
            events.add(event);
        }

        List<RuntimeEvent> events() {
            return events;
        }
    }
}
