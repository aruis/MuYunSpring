package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.database.core.orm.OrmException;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.module.metadata.EntityCapability;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.module.metadata.EntityRelationDefinition;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                    .contains("id", "tenant_id", "version", "deleted", "created_by", "created_at", "updated_by", "updated_at",
                            "code", "name", "amount", "signed_at");
            assertThat(primaryKeys(connection)).containsExactly("id");
            assertThat(uniqueIndexColumns(connection, "app_contract")).contains(List.of("tenant_id", "code"));
        }
    }

    @Test
    void shouldRunDynamicAbilitiesOnRealDatabase() {
        ModuleDefinition module = invoiceModule();
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations);
        DynamicModulePublisher publisher = new DynamicModulePublisher(schemaService, runtime);
        publisher.publish(module);

        DynamicEntityService invoiceService = runtime.entityService("sales.invoice", "invoice");
        DynamicEntityService lineService = runtime.entityService("sales.invoice", "invoice_line");
        String rootId;
        String firstChildId;
        String secondChildId;
        String lineId;

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            DynamicRecord root = runtime.newRecord("sales.invoice", "invoice")
                    .setValue("code", "INV-ROOT")
                    .setValue("title", "Root invoice");
            rootId = invoiceService.insert(root);

            DynamicRecord firstChild = runtime.newRecord("sales.invoice", "invoice")
                    .setValue("code", "INV-CHILD-1")
                    .setValue("title", "First child");
            firstChild.setParentId(rootId);
            firstChildId = invoiceService.insert(firstChild);

            DynamicRecord secondChild = runtime.newRecord("sales.invoice", "invoice")
                    .setValue("code", "INV-CHILD-2")
                    .setValue("title", "Second child");
            secondChild.setParentId(rootId);
            secondChildId = invoiceService.insert(secondChild);

            invoiceService.reorder(List.of(secondChildId, firstChildId));
            assertThat(invoiceService.sortedList(Criteria.of().eq("parentId", rootId)).stream().map(DynamicRecord::getId))
                    .containsExactly(secondChildId, firstChildId);
            assertThat(invoiceService.children(rootId).stream().map(record -> record.getValue("title")))
                    .containsExactly("Second child", "First child");
            assertThat(invoiceService.title(firstChildId)).isEqualTo("First child");
            assertThat(invoiceService.referenceOptions(Criteria.of().eq("parentId", rootId), PageRequest.of(1, 10)).getRecords())
                    .extracting("id")
                    .containsExactlyInAnyOrder(secondChildId, firstChildId);

            DynamicRecord invoiceWithLine = runtime.newRecord("sales.invoice", "invoice")
                    .setValue("code", "INV-LINE")
                    .setValue("title", "Invoice with line");
            DynamicRecord line = runtime.newRecord("sales.invoice", "invoice_line")
                    .setValue("title", "Line 001");
            invoiceWithLine.setChildren("lines", List.of(line));
            String invoiceWithLineId = invoiceService.insert(invoiceWithLine);
            lineId = line.getId();

            DynamicRecord loadedLine = invoiceService.select(invoiceWithLineId).getChildren("lines").getFirst();
            assertThat(loadedLine)
                    .extracting(child -> child.getValue("title"), child -> child.getValue("invoiceId"))
                    .containsExactly("Line 001", invoiceWithLineId);
            assertThat(lineService.collectReferenceIdsBySourceNamespace(loadedLine))
                    .containsEntry("sales.invoice.invoice", java.util.Set.of(invoiceWithLineId));
            assertThat(lineService.select(lineId))
                    .extracting(child -> child.getValue("invoiceId"))
                    .isEqualTo(invoiceWithLineId);
            assertThat(invoiceService.select(invoiceWithLineId).getChildren("lines"))
                    .hasSize(1)
                    .first()
                    .extracting(child -> child.getValue("title"))
                    .isEqualTo("Line 001");

            assertThatThrownBy(() -> invoiceService.insert(runtime.newRecord("sales.invoice", "invoice")
                    .setValue("code", "INV-ROOT")
                    .setValue("title", "Duplicate in tenant A")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("duplicate key");

            assertThat(invoiceService.delete(invoiceWithLineId)).isEqualTo(1);
            assertThat(lineService.select(lineId)).isNull();
            assertThat(lineService.selectIgnoreSoftDelete(lineId)).isNotNull();
        }

        try (TenantContext.Scope ignored = TenantContext.use("tenant-b")) {
            assertThat(invoiceService.select(rootId)).isNull();
            assertThat(invoiceService.insert(runtime.newRecord("sales.invoice", "invoice")
                    .setValue("code", "INV-ROOT")
                    .setValue("title", "Same code in tenant B"))).hasSize(32);
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

        DynamicRecord staleUpdate = new DynamicRecord(entity)
                .setValue("name", "Stale Contract");
        staleUpdate.setId(id);
        staleUpdate.setVersion(0);
        assertThatThrownBy(() -> entityService.update(staleUpdate))
                .isInstanceOf(OptimisticLockException.class);

        DynamicRecord staleDelete = new DynamicRecord(entity);
        staleDelete.setId(id);
        staleDelete.setVersion(0);
        assertThatThrownBy(() -> entityService.delete(staleDelete))
                .isInstanceOf(OptimisticLockException.class);

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

    private List<List<String>> uniqueIndexColumns(Connection connection, String tableName) throws Exception {
        try (var indexes = connection.getMetaData().getIndexInfo(null, "public", tableName, true, false)) {
            Map<String, List<String>> columnsByIndex = new LinkedHashMap<>();
            while (indexes.next()) {
                String name = indexes.getString("INDEX_NAME");
                String column = indexes.getString("COLUMN_NAME");
                if (name != null && column != null) {
                    columnsByIndex.computeIfAbsent(name, ignored -> new ArrayList<>()).add(column);
                }
            }
            return new ArrayList<>(columnsByIndex.values());
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws Exception {
        try (var tables = connection.getMetaData().getTables(null, "public", tableName, null)) {
            return tables.next();
        }
    }

    private ModuleDefinition invoiceModule() {
        return new ModuleDefinition(
                "sales.invoice",
                "Invoice",
                List.of(invoiceEntity(), invoiceLineEntity()),
                List.of(EntityRelationDefinition.child("lines", "invoice", "invoice_line", "invoiceId")
                        .withAutoPopulate()
                        .withAutoDeleteWithParent()),
                List.of(EntityReferenceDefinition.from("invoice_line", "invoiceId", "sales.invoice.invoice"))
        );
    }

    private EntityDefinition invoiceEntity() {
        return new EntityDefinition(
                "invoice",
                "app_invoice_ability_it",
                "Invoice",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required().unique(),
                        FieldDefinition.titleField().required(),
                        FieldDefinition.parentId(),
                        FieldDefinition.sortOrder()
                )
        ).withCapabilities(EntityCapability.CRUD, EntityCapability.TREE, EntityCapability.SORT, EntityCapability.REFERENCE);
    }

    private EntityDefinition invoiceLineEntity() {
        return new EntityDefinition(
                "invoice_line",
                "app_invoice_line_ability_it",
                "Invoice Line",
                List.of(
                        FieldDefinition.string("invoiceId", "Invoice").column("invoice_id").length(64).required().indexed(),
                        FieldDefinition.titleField().required()
                )
        ).withCapabilities(EntityCapability.CRUD, EntityCapability.REFERENCE);
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
