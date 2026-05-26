package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.database.core.orm.OrmException;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = DynamicSchemaServiceIT.TestApplication.class)
class DynamicSchemaServiceIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("muyun.database.default-schema", () -> "public");
    }

    private final DynamicSchemaService schemaService;
    private final IDatabaseOperations<?> operations;
    private final DataSource dataSource;

    @Autowired
    DynamicSchemaServiceIT(DynamicSchemaService schemaService, IDatabaseOperations<?> operations, DataSource dataSource) {
        this.schemaService = schemaService;
        this.operations = operations;
        this.dataSource = dataSource;
    }

    @Test
    void shouldCreateDynamicTableAndKeepSecondEnsureIdempotent() throws Exception {
        EntityDefinition entity = new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required().unique(),
                        FieldDefinition.string("name", "Name").length(128).required(),
                        FieldDefinition.decimal("amount", "Amount").precision(18, 2),
                        FieldDefinition.timestamp("signedAt", "Signed At").column("signed_at").indexed()
                )
        );

        assertThat(schemaService.ensureTable(entity)).isTrue();
        assertThat(schemaService.ensureTable(entity)).isFalse();

        try (Connection connection = dataSource.getConnection()) {
            assertThat(columns(connection))
                    .contains("id", "version", "deleted", "created_by", "created_at", "updated_by", "updated_at",
                            "code", "name", "amount", "signed_at");
            assertThat(primaryKeys(connection)).containsExactly("id");
            assertThat(uniqueIndexes(connection)).anyMatch(indexName -> indexName.contains("code"));
        }
    }

    @Test
    void shouldRunDynamicRecordMinimalDataAccessLoopOnRealDatabase() {
        EntityDefinition entity = entity("app_contract_record_it");
        schemaService.ensureTable(entity);
        DynamicRecordDao dao = new DynamicRecordDao(operations, entity);
        DynamicEntityService entityService = new DynamicEntityService(dao, "sales.contract");

        DynamicRecord record = new DynamicRecord(entity)
                .setValue("code", "C-IT-001")
                .setValue("name", "Integration Contract")
                .setValue("amount", BigDecimal.valueOf(1234, 2));

        String id = entityService.insert(record);

        assertThat(entityService.select(id).getValue("code")).isEqualTo("C-IT-001");
        assertThat(entityService.pageQuery(Criteria.of().eq("code", "C-IT-001"), PageRequest.of(1, 10), Sort.asc("name")).getRecords())
                .hasSize(1);
        assertThat(entityService.pageQuery(Criteria.of().eq("code", "C-IT-001"), PageRequest.of(1, 10)).getTotal())
                .isEqualTo(1);
        assertThat(entityService.count(Criteria.of().eq("code", "C-IT-001"))).isEqualTo(1);

        record.setValue("name", "Updated Contract");
        entityService.update(record);
        assertThat(entityService.select(id).getVersion()).isEqualTo(1);
        assertThat(entityService.select(id).getValue("name")).isEqualTo("Updated Contract");

        assertThat(entityService.delete(id)).isEqualTo(1);
        assertThat(entityService.select(id)).isNull();
        assertThat(entityService.count(Criteria.of().eq("code", "C-IT-001"))).isZero();
        assertThat(dao.count(Criteria.of().eq("code", "C-IT-001"))).isEqualTo(1);
    }

    @Test
    void shouldPublishModuleThenRunDynamicRecordOnRealDatabase() {
        ModuleDefinition module = new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(entity("app_contract_publish_it"))
        );
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations);
        DynamicModulePublisher publisher = new DynamicModulePublisher(schemaService, runtime);

        DynamicModulePublishResult result = publisher.publish(module);
        DynamicRecordService recordService = new DynamicRecordService(runtime);
        DynamicRecord record = recordService.newRecord("sales.contract", "contract")
                .setValue("code", "C-PUBLISH-001")
                .setValue("name", "Published Contract")
                .setValue("amount", BigDecimal.valueOf(5678, 2));

        String id = recordService.create("sales.contract", "contract", record);

        assertThat(result.changed()).isTrue();
        assertThat(result.migrations()).containsKey("contract");
        assertThat(recordService.select("sales.contract", "contract", id).getValue("code")).isEqualTo("C-PUBLISH-001");
        assertThat(recordService.count("sales.contract", "contract", Criteria.of().eq("code", "C-PUBLISH-001"))).isEqualTo(1);
    }

    @Test
    void shouldSupportDryRunAndStrictDynamicSchemaMigration() throws Exception {
        EntityDefinition dryRunEntity = entity("app_contract_dry_run_it");

        var dryRun = schemaService.ensureTable(dryRunEntity, MigrationOptions.dryRun());

        assertThat(dryRun.isDryRun()).isTrue();
        assertThat(dryRun.isChanged()).isTrue();
        try (Connection connection = dataSource.getConnection()) {
            assertThat(tableExists(connection, "app_contract_dry_run_it")).isFalse();
        }

        EntityDefinition looseEntity = entity("app_contract_strict_it", false);
        EntityDefinition strictEntity = entity("app_contract_strict_it", true);
        schemaService.ensureTable(looseEntity);

        assertThatThrownBy(() -> schemaService.ensureTable(strictEntity, MigrationOptions.strict()))
                .isInstanceOf(OrmException.class)
                .extracting("code")
                .isEqualTo(OrmException.Code.STRICT_MIGRATION_REJECTED);
    }

    private EntityDefinition entity(String tableName) {
        return entity(tableName, true);
    }

    private EntityDefinition entity(String tableName, boolean nameRequired) {
        FieldDefinition name = FieldDefinition.string("name", "Name").length(128);
        if (nameRequired) {
            name = name.required();
        }
        return new EntityDefinition(
                "contract",
                tableName,
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required().unique(),
                        name,
                        FieldDefinition.decimal("amount", "Amount").precision(18, 2),
                        FieldDefinition.timestamp("signedAt", "Signed At").column("signed_at").indexed()
                )
        );
    }

    private List<String> columns(Connection connection) throws Exception {
        try (var columns = connection.getMetaData().getColumns(null, "public", "app_contract", null)) {
            java.util.ArrayList<String> names = new java.util.ArrayList<>();
            while (columns.next()) {
                names.add(columns.getString("COLUMN_NAME"));
            }
            return names;
        }
    }

    private List<String> primaryKeys(Connection connection) throws Exception {
        try (var keys = connection.getMetaData().getPrimaryKeys(null, "public", "app_contract")) {
            java.util.ArrayList<String> names = new java.util.ArrayList<>();
            while (keys.next()) {
                names.add(keys.getString("COLUMN_NAME"));
            }
            return names;
        }
    }

    private List<String> uniqueIndexes(Connection connection) throws Exception {
        try (var indexes = connection.getMetaData().getIndexInfo(null, "public", "app_contract", true, false)) {
            java.util.ArrayList<String> names = new java.util.ArrayList<>();
            while (indexes.next()) {
                String name = indexes.getString("INDEX_NAME");
                if (name != null) {
                    names.add(name);
                }
            }
            return names;
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws Exception {
        try (var tables = connection.getMetaData().getTables(null, "public", tableName, null)) {
            return tables.next();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        DataSource dataSource() {
            return org.springframework.boot.jdbc.DataSourceBuilder.create()
                    .url(postgres.getJdbcUrl())
                    .username(postgres.getUsername())
                    .password(postgres.getPassword())
                    .driverClassName(postgres.getDriverClassName())
                    .build();
        }

        @Bean
        DynamicSchemaService dynamicSchemaService(IDatabaseOperations<?> operations) {
            return new DynamicSchemaService(operations);
        }
    }
}
