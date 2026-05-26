package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.database.core.orm.MigrationResult;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinitionException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicSchemaServiceTest {

    @Test
    void shouldEnsureModuleTablesInDefinitionOrder() {
        RecordingDynamicSchemaService service = new RecordingDynamicSchemaService();
        ModuleDefinition module = new ModuleDefinition(
                "contract.app",
                "Contract App",
                List.of(
                        entity("contract", "app_contract"),
                        entity("invoice", "app_invoice")
                )
        );

        assertThat(service.ensureModule(module))
                .containsEntry("contract", true)
                .containsEntry("invoice", false);
        assertThat(service.ensuredEntities).containsExactly("contract", "invoice");
    }

    @Test
    void shouldValidateModuleBeforeEnsuringTables() {
        RecordingDynamicSchemaService service = new RecordingDynamicSchemaService();
        ModuleDefinition module = new ModuleDefinition(
                "contract.app",
                "Contract App",
                List.of(
                        entity("contract", "app_contract"),
                        entity("contract_copy", "app_contract")
                )
        );

        assertThatThrownBy(() -> service.ensureModule(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("duplicate table name");
        assertThat(service.ensuredEntities).isEmpty();
    }

    private EntityDefinition entity(String code, String tableName) {
        return new EntityDefinition(
                code,
                tableName,
                code,
                List.of(FieldDefinition.string("name", "Name").length(128))
        );
    }

    private static class RecordingDynamicSchemaService extends DynamicSchemaService {
        private final List<String> ensuredEntities = new ArrayList<>();

        RecordingDynamicSchemaService() {
            super(null);
        }

        @Override
        public Map<String, MigrationResult> ensureModule(ModuleDefinition module, MigrationOptions options) {
            new net.ximatai.muyun.spring.module.metadata.ModuleDefinitionValidator().validate(module);
            Map<String, MigrationResult> results = new LinkedHashMap<>();
            for (EntityDefinition entity : module.entities()) {
                ensuredEntities.add(entity.code());
                results.put(entity.code(), new MigrationResult(ensuredEntities.size() == 1, false, false, List.of()));
            }
            return results;
        }
    }
}
