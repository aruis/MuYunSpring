package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.database.core.orm.OrmException;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.database.core.orm.SqlRawCondition;
import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.spring.ability.ReferenceTarget;
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
import java.time.Instant;
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
            assertThat(lineService.collectReferenceIdsByTarget(loadedLine))
                    .containsEntry(ReferenceTarget.of("sales.invoice", "invoice"), java.util.Set.of(invoiceWithLineId));
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
    void shouldRunComplexCriteriaContractOnRealDatabase() {
        EntityDefinition entity = entity("app_contract_criteria_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        schemaService.ensureTable(entity);
        DynamicEntityService service = new DynamicEntityService(new DynamicRecordDao(operations, entity), "sales.contract.criteria");

        try (TenantContext.Scope ignored = TenantContext.use("tenant-criteria")) {
            insertContract(service, entity, "C-CR-001", "Alpha", "2026-01-01T00:00:00Z", "10.00");
            insertContract(service, entity, "C-CR-002", "Beta", "2026-01-02T00:00:00Z", "20.00");
            insertContract(service, entity, "C-CR-003", "Gamma", "2026-01-03T00:00:00Z", "15.00");
            String deletedId = insertContract(service, entity, "C-CR-004", "Deleted", "2026-01-04T00:00:00Z", "15.00");
            insertContract(service, entity, "C-CR-005", "Blocked", "2026-01-05T00:00:00Z", "15.00");
            insertContract(service, entity, "C-CR-006", "Out of Range", "2026-01-06T00:00:00Z", "99.00");
            insertContract(service, entity, "C-CR-007", "No Signed At", null, "15.00");
            service.delete(deletedId);
        }

        try (TenantContext.Scope ignored = TenantContext.use("tenant-other")) {
            insertContract(service, entity, "C-CR-008", "Other Tenant", "2026-01-08T00:00:00Z", "15.00");
        }

        try (TenantContext.Scope ignored = TenantContext.use("tenant-criteria")) {
            Criteria criteria = Criteria.of()
                    .in("code", List.of("C-CR-001", "C-CR-002", "C-CR-003", "C-CR-004", "C-CR-005", "C-CR-006", "C-CR-007", "C-CR-008"))
                    .notIn("code", List.of("C-CR-003"))
                    .between("amount", new BigDecimal("10.00"), new BigDecimal("30.00"))
                    .isNotNull("signedAt")
                    .raw(SqlRawCondition.of("\"name\" <> :blocked", Map.of("blocked", "Blocked")));

            assertThat(service.pageQuery(criteria, PageRequest.of(1, 1), Sort.desc("amount")).getRecords())
                    .extracting(record -> record.getValue("code"))
                    .containsExactly("C-CR-002");
            assertThat(service.count(criteria)).isEqualTo(2);
            assertThat(service.pageQuery(Criteria.of().in("code", List.of()), PageRequest.of(1, 10)).getRecords())
                    .isEmpty();
            assertThat(service.pageQuery(Criteria.of().notIn("code", List.of()), PageRequest.of(1, 10)).getTotal())
                    .isEqualTo(6);

            assertThatThrownBy(() -> service.count(Criteria.of().eq("missingField", "x")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unknown dynamic field or column");
            assertThatThrownBy(() -> service.count(Criteria.of().raw(SqlRawCondition.of("\"name\" = ?", Map.of()))))
                    .isInstanceOf(OrmException.class)
                    .extracting("code")
                    .isEqualTo(OrmException.Code.INVALID_CRITERIA);
        }
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
    void shouldPreviewAndPublishDynamicModuleSchemaEvolutionOnRealDatabase() throws Exception {
        String suffix = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String tableName = "app_contract_evolve_" + suffix;
        String moduleAlias = "sales.evolve_" + suffix;
        ModuleDefinition initialModule = new ModuleDefinition(
                moduleAlias,
                "Contract",
                List.of(new EntityDefinition(
                        "contract",
                        tableName,
                        "Contract",
                        List.of(FieldDefinition.string("code", "Code").length(64).required().unique())
                ))
        );
        ModuleDefinition evolvedModule = new ModuleDefinition(
                moduleAlias,
                "Contract",
                List.of(new EntityDefinition(
                        "contract",
                        tableName,
                        "Contract",
                        List.of(
                                FieldDefinition.string("code", "Code").length(64).required().unique(),
                                FieldDefinition.string("name", "Name").length(128),
                                FieldDefinition.decimal("amount", "Amount").precision(18, 2)
                        )
                ))
        );
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations);
        DynamicModulePublisher publisher = new DynamicModulePublisher(schemaService, runtime);
        DynamicRecordService recordService = new DynamicRecordService(runtime);

        publisher.publish(initialModule);
        String firstId = recordService.create(moduleAlias, "contract", runtime.newRecord(moduleAlias, "contract")
                .setValue("code", "C-EVOLVE-001"));

        DynamicModulePublishResult dryRun = publisher.preview(evolvedModule);

        assertThat(dryRun.changed()).isTrue();
        assertThat(dryRun.dryRun()).isTrue();
        assertThat(dryRun.statementsByEntity().get("contract"))
                .anyMatch(sql -> sql.contains(" add ") && sql.contains("\"name\""))
                .anyMatch(sql -> sql.contains(" add ") && sql.contains("\"amount\""));
        assertThat(runtime.registry().requireEntity(moduleAlias, "contract").fields())
                .extracting(FieldDefinition::fieldName)
                .containsExactly("code");
        try (Connection connection = dataSource.getConnection()) {
            assertThat(columns(connection, tableName)).doesNotContain("name", "amount");
        }

        DynamicModulePublishResult published = publisher.publish(evolvedModule);

        assertThat(published.changed()).isTrue();
        assertThat(runtime.registry().requireEntity(moduleAlias, "contract").fields())
                .extracting(FieldDefinition::fieldName)
                .containsExactly("code", "name", "amount");
        String secondId = recordService.create(moduleAlias, "contract", runtime.newRecord(moduleAlias, "contract")
                .setValue("code", "C-EVOLVE-002")
                .setValue("name", "Evolved Contract")
                .setValue("amount", BigDecimal.valueOf(4567, 2)));
        assertThat(recordService.select(moduleAlias, "contract", firstId).getValue("name")).isNull();
        assertThat((BigDecimal) recordService.select(moduleAlias, "contract", secondId).getValue("amount"))
                .isEqualByComparingTo(BigDecimal.valueOf(4567, 2));
        try (Connection connection = dataSource.getConnection()) {
            assertThat(columns(connection, tableName)).contains("name", "amount");
        }

        ModuleDefinition removedFieldModule = new ModuleDefinition(
                moduleAlias,
                "Contract",
                List.of(new EntityDefinition(
                        "contract",
                        tableName,
                        "Contract",
                        List.of(
                                FieldDefinition.string("code", "Code").length(64).required().unique(),
                                FieldDefinition.decimal("amount", "Amount").precision(18, 2)
                        )
                ))
        );

        DynamicModulePublishResult dropDryRun = publisher.preview(removedFieldModule);

        assertThat(dropDryRun.dryRun()).isTrue();
        assertThat(dropDryRun.hasNonAdditiveChanges()).isTrue();
        assertThat(dropDryRun.statementsByEntity().get("contract"))
                .anyMatch(sql -> sql.contains("drop column \"name\""));
        assertThat(runtime.registry().requireEntity(moduleAlias, "contract").fields())
                .extracting(FieldDefinition::fieldName)
                .containsExactly("code", "name", "amount");
        assertThatThrownBy(() -> publisher.publish(removedFieldModule, MigrationOptions.strict()))
                .isInstanceOf(OrmException.class)
                .extracting("code")
                .isEqualTo(OrmException.Code.STRICT_MIGRATION_REJECTED);
        assertThat(runtime.registry().requireEntity(moduleAlias, "contract").fields())
                .extracting(FieldDefinition::fieldName)
                .containsExactly("code", "name", "amount");

        DynamicModulePublishResult dropPublished = publisher.publish(removedFieldModule);

        assertThat(dropPublished.migrations().get("contract").hasNonAdditiveChanges()).isTrue();
        assertThat(runtime.registry().requireEntity(moduleAlias, "contract").fields())
                .extracting(FieldDefinition::fieldName)
                .containsExactly("code", "amount");
        try (Connection connection = dataSource.getConnection()) {
            assertThat(columns(connection, tableName)).doesNotContain("name");
        }
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

    private String insertContract(DynamicEntityService service,
                                  EntityDefinition entity,
                                  String code,
                                  String name,
                                  String signedAt,
                                  String amount) {
        DynamicRecord record = new DynamicRecord(entity)
                .setValue("code", code)
                .setValue("name", name)
                .setValue("amount", new BigDecimal(amount));
        if (signedAt != null) {
            record.setValue("signedAt", Instant.parse(signedAt));
        }
        return service.insert(record);
    }

    private List<String> columns(Connection connection) throws Exception {
        return columns(connection, "app_contract");
    }

    private List<String> columns(Connection connection, String tableName) throws Exception {
        try (var columns = connection.getMetaData().getColumns(null, "public", tableName, null)) {
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
                List.of(EntityReferenceDefinition.to("invoice_line", "invoiceId", ReferenceTarget.of("sales.invoice", "invoice")))
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
