package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.database.core.orm.SqlRawCondition;
import net.ximatai.muyun.spring.ability.AbilityException;
import net.ximatai.muyun.spring.ability.ReferenceOption;
import net.ximatai.muyun.spring.module.metadata.EntityCapability;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicRecordDaoTest {
    private static final String SCHEMA = "public";
    private static final String TABLE = "app_contract";

    @Test
    void shouldInsertWithTableColumnsAndEntityDefaults() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq(TABLE), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        DynamicRecord record = new DynamicRecord(contractEntity())
                .setValue("code", "C-001")
                .setValue("amount", BigDecimal.TEN);
        record.setTenantId("tenant-a");

        String id = entityService(operations).insert(record);

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations).insertItem(eq(SCHEMA), eq(TABLE), body.capture());
        assertThat(id).hasSize(32);
        assertThat(body.getValue())
                .containsEntry("id", id)
                .containsEntry("tenant_id", "tenant-a")
                .containsEntry("code", "C-001")
                .containsEntry("amount", BigDecimal.TEN)
                .containsEntry("version", 0)
                .containsEntry("deleted", Boolean.FALSE);
        assertThat(body.getValue()).containsEntry("deleted_at", null);
        assertThat(body.getValue()).containsKeys("created_at", "updated_at");
    }

    @Test
    void shouldRunLifecycleHooksAroundCrudOperations() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq(TABLE), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(Map.of(
                "id", "contract-1",
                "code", "C-001",
                "amount", BigDecimal.TEN,
                "deleted", Boolean.FALSE,
                "version", 2
        )));
        RecordingLifecycle lifecycle = new RecordingLifecycle();
        DynamicRecordDao dao = new DynamicRecordDao(operations, contractEntity());
        DynamicEntityService entityService = new DynamicEntityService(dao, "sales.contract", lifecycle);

        DynamicRecord inserted = new DynamicRecord(contractEntity())
                .setValue("amount", BigDecimal.TEN);
        entityService.insert(inserted);
        DynamicRecord selected = entityService.select("contract-1");
        entityService.update(selected.setValue("amount", BigDecimal.ONE));
        entityService.delete("contract-1");

        assertThat(lifecycle.events)
                .containsExactly(
                        "beforeInsert:HOOK-CODE",
                        "afterSelect:contract-1",
                        "beforeUpdate:3",
                        "beforeDelete:contract-1"
                );
        ArgumentCaptor<Map<String, Object>> insertBody = mapCaptor();
        verify(operations).insertItem(eq(SCHEMA), eq(TABLE), insertBody.capture());
        assertThat(insertBody.getValue()).containsEntry("code", "HOOK-CODE");
    }

    @Test
    void shouldNotRunAfterSelectForInternalReadsOrListQueries() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.row(anyString(), anyMap())).thenReturn(Map.of("total_count", 1));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(Map.of(
                "id", "contract-1",
                "code", "C-001",
                "amount", BigDecimal.TEN,
                "deleted", Boolean.FALSE,
                "version", 2
        )));
        RecordingLifecycle lifecycle = new RecordingLifecycle();
        DynamicRecordDao dao = new DynamicRecordDao(operations, contractEntity());
        DynamicEntityService entityService = new DynamicEntityService(dao, "sales.contract", lifecycle);

        DynamicRecord partial = new DynamicRecord(contractEntity()).setValue("amount", BigDecimal.ONE);
        partial.setId("contract-1");
        entityService.update(partial);
        entityService.delete("contract-1");
        dao.query(Criteria.of().eq("code", "C-001"), PageRequest.of(1, 10));
        dao.pageQuery(Criteria.of().eq("code", "C-001"), PageRequest.of(1, 10));
        dao.count(Criteria.of().eq("code", "C-001"));

        assertThat(lifecycle.events)
                .containsExactly(
                        "beforeUpdate:3",
                        "beforeDelete:contract-1"
                );
    }

    @Test
    void shouldUpdateByTableColumnsAndIncrementVersion() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(Map.of(
                "id", "contract-1",
                "code", "C-000",
                "amount", BigDecimal.ONE,
                "deleted", Boolean.FALSE,
                "version", 2
        )));
        DynamicRecord record = new DynamicRecord(contractEntity())
                .setValue("code", "C-001")
                .setValue("amount", BigDecimal.TEN);
        record.setId("contract-1");
        record.setTenantId("tenant-a");
        record.setVersion(3);

        entityService(operations).update(record);

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        ArgumentCaptor<Map<String, Object>> where = mapCaptor();
        verify(operations).patchUpdateItemWhere(eq(SCHEMA), eq(TABLE), body.capture(), where.capture());
        assertThat(body.getValue())
                .containsEntry("code", "C-001")
                .containsEntry("amount", BigDecimal.TEN)
                .containsEntry("version", 4);
        assertThat(where.getValue())
                .containsEntry("id", "contract-1")
                .containsEntry("version", 3);
        assertThat(body.getValue())
                .containsKey("updated_at")
                .doesNotContainKeys("tenant_id", "created_at", "created_by");
    }

    @Test
    void shouldResolveCurrentVersionWhenUpdatingPartialRecord() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.row(anyString(), anyMap())).thenReturn(Map.of("total_count", 0));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(Map.of(
                "id", "contract-1",
                "code", "C-001",
                "amount", BigDecimal.TEN,
                "deleted", Boolean.FALSE,
                "version", 7
        )));
        DynamicRecord record = new DynamicRecord(contractEntity())
                .setValue("amount", BigDecimal.ONE);
        record.setId("contract-1");

        entityService(operations).update(record);

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        ArgumentCaptor<Map<String, Object>> where = mapCaptor();
        verify(operations).patchUpdateItemWhere(eq(SCHEMA), eq(TABLE), body.capture(), where.capture());
        assertThat(body.getValue())
                .containsEntry("amount", BigDecimal.ONE)
                .containsEntry("version", 8)
                .doesNotContainKeys("created_at", "created_by", "code");
        assertThat(where.getValue())
                .containsEntry("id", "contract-1")
                .containsEntry("version", 7);
    }

    @Test
    void shouldRejectDynamicUpdateWhenExpectedVersionDoesNotMatch() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.patchUpdateItemWhere(anyString(), anyString(), anyMap(), anyMap())).thenReturn(0);
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(Map.of(
                "id", "contract-1",
                "code", "C-001",
                "amount", BigDecimal.TEN,
                "deleted", Boolean.FALSE,
                "version", 2
        )));
        DynamicRecord record = new DynamicRecord(contractEntity())
                .setValue("amount", BigDecimal.ONE);
        record.setId("contract-1");
        record.setVersion(1);

        assertThatThrownBy(() -> entityService(operations).update(record))
                .isInstanceOf(net.ximatai.muyun.spring.ability.OptimisticLockException.class)
                .hasMessageContaining("version conflict");

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        ArgumentCaptor<Map<String, Object>> where = mapCaptor();
        verify(operations).patchUpdateItemWhere(eq(SCHEMA), eq(TABLE), body.capture(), where.capture());
        assertThat(where.getValue()).containsEntry("version", 1);
    }

    @Test
    void shouldRejectPartialUpdateWhenOnlyDeletedRecordExists() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap())).thenReturn(List.of());
        DynamicRecord record = new DynamicRecord(contractEntity())
                .setValue("amount", BigDecimal.ONE);
        record.setId("contract-1");

        assertThat(entityService(operations).update(record)).isZero();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations).query(sql.capture(), anyMap());
        assertThat(sql.getValue())
                .contains("\"id\" =")
                .contains("\"deleted\" =")
                .contains("\"deleted\" IS NULL");
        verify(operations, never()).patchUpdateItemWhere(anyString(), anyString(), anyMap(), anyMap());
    }

    @Test
    void shouldSoftDeleteActiveRecordAndIncrementVersion() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(Map.of(
                "id", "contract-1",
                "code", "C-001",
                "amount", BigDecimal.TEN,
                "deleted", Boolean.FALSE,
                "version", 2
        )));

        entityService(operations).delete("contract-1");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations).query(sql.capture(), anyMap());
        assertThat(sql.getValue())
                .contains("\"id\" =")
                .doesNotContain("\"deleted\"");

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        ArgumentCaptor<Map<String, Object>> where = mapCaptor();
        verify(operations).patchUpdateItemWhere(eq(SCHEMA), eq(TABLE), body.capture(), where.capture());
        assertThat(body.getValue())
                .containsEntry("deleted", Boolean.TRUE)
                .containsEntry("version", 3);
        assertThat(body.getValue()).containsKey("deleted_at");
        assertThat(where.getValue()).containsEntry("version", 2);
        assertThat(body.getValue()).doesNotContainKeys("code", "amount", "created_at", "created_by");
    }

    @Test
    void shouldNotExposeDynamicDeleteByIdBypass() {
        DynamicRecordDao dao = dao(operations());

        assertThatThrownBy(() -> dao.deleteById("contract-1"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("DynamicEntityService");
    }

    @Test
    void shouldRejectDynamicEntityServiceWithoutPlatformModuleAlias() {
        assertThatThrownBy(() -> new DynamicEntityService(dao(operations()), "contract"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("platform module alias");
    }

    @Test
    void shouldUseSharedCriteriaCompilerForRawCriteria() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap())).thenReturn(List.of());
        DynamicRecordDao dao = dao(operations);

        dao.query(
                Criteria.of().raw(SqlRawCondition.of("\"code\" = :code", Map.of("code", "C-001"))),
                PageRequest.of(1, 10)
        );

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> params = mapCaptor();
        verify(operations).query(sql.capture(), params.capture());
        assertThat(sql.getValue()).contains("\"code\" = :sq");
        assertThat(params.getValue()).containsValue("C-001");
    }

    @Test
    void shouldQueryPageAndCountWithSoftDeleteScopeAndSortMapping() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.row(anyString(), anyMap())).thenReturn(Map.of("total_count", 1));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(Map.of(
                "id", "contract-1",
                "code", "C-001",
                "deleted", Boolean.FALSE,
                "version", 0
        )));
        DynamicRecordDao dao = dao(operations);
        DynamicEntityService entityService = new DynamicEntityService(dao, "sales.contract");

        assertThat(entityService.pageQuery(Criteria.of().eq("tenantId", "tenant-a").eq("code", "C-001"),
                        PageRequest.of(1, 10),
                        Sort.desc("amount")).getRecords())
                .hasSize(1)
                .first()
                .extracting(record -> record.getValue("code"))
                .isEqualTo("C-001");
        assertThat(entityService.pageQuery(Criteria.of().eq("code", "C-001"), PageRequest.of(1, 10)).getTotal())
                .isEqualTo(1);
        assertThat(entityService.count(Criteria.of().eq("code", "C-001"))).isEqualTo(1);

        ArgumentCaptor<String> querySql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.times(2)).query(querySql.capture(), anyMap());
        assertThat(querySql.getAllValues().getFirst())
                .contains("FROM \"public\".\"app_contract\"")
                .contains("\"tenant_id\" =")
                .contains("\"code\" =")
                .contains("\"deleted\" =")
                .contains("\"deleted\" IS NULL")
                .contains("ORDER BY \"amount\" DESC")
                .contains("LIMIT :limit OFFSET :offset");

        ArgumentCaptor<String> countSql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.times(3)).row(countSql.capture(), anyMap());
        assertThat(countSql.getAllValues().getFirst())
                .startsWith("SELECT COUNT(*) AS total_count FROM \"public\".\"app_contract\"")
                .contains("\"code\" =")
                .contains("\"deleted\" =");
    }

    @Test
    void shouldResolveDeletedAtAsDynamicStandardField() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap())).thenReturn(List.of());
        DynamicRecordDao dao = dao(operations);

        dao.query(Criteria.of().eq("deletedAt", Instant.parse("2026-01-02T00:00:00Z")), PageRequest.of(1, 10));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations).query(sql.capture(), anyMap());
        assertThat(sql.getValue()).contains("\"deleted_at\" =");
    }

    @Test
    void shouldKeepDaoRawAndServiceScoped() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(Map.of(
                "id", "contract-1",
                "tenant_id", "tenant-a",
                "code", "C-001",
                "amount", BigDecimal.TEN,
                "deleted", Boolean.TRUE,
                "version", 2
        )));
        DynamicRecordDao dao = dao(operations);
        DynamicEntityService entityService = new DynamicEntityService(dao, "sales.contract");

        DynamicRecord raw = dao.findById("contract-1");
        assertThat(raw.getDeleted()).isTrue();
        assertThat(raw.getTenantId()).isEqualTo("tenant-a");
        assertThat(entityService.select("contract-1")).isNull();
        assertThat(entityService.count(Criteria.of().eq("code", "C-001"))).isZero();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.times(2)).query(sql.capture(), anyMap());
        assertThat(sql.getAllValues().get(0)).doesNotContain("\"deleted\"");
        assertThat(sql.getAllValues().get(1)).contains("\"deleted\"");
        ArgumentCaptor<String> countSql = ArgumentCaptor.forClass(String.class);
        verify(operations).row(countSql.capture(), anyMap());
        assertThat(countSql.getValue())
                .contains("\"deleted\" =")
                .contains("\"deleted\" IS NULL");
    }

    @Test
    void shouldCreateEntityServiceThroughDynamicRecordRuntime() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq(TABLE), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        DynamicEntityService entityService = new DynamicRecordRuntime(operations)
                .register(new ModuleDefinition("sales.contract", "Contract", List.of(contractEntity())))
                .entityService("sales.contract", "contract");
        DynamicRecord record = new DynamicRecord(contractEntity())
                .setValue("code", "C-001")
                .setValue("amount", BigDecimal.TEN);

        String id = entityService.insert(record);

        assertThat(id).hasSize(32);
        verify(operations).insertItem(eq(SCHEMA), eq(TABLE), anyMap());
    }

    @Test
    void shouldReorderAndMoveDynamicRecordsByDeclaredSortField() {
        IDatabaseOperations<Object> operations = operations();
        stubSortableRows(operations);
        DynamicEntityService entityService = new DynamicEntityService(new DynamicRecordDao(operations, sortableEntity()), "sales.contract");

        assertThat(entityService.sortedList(Criteria.of()).stream().map(DynamicRecord::getId))
                .containsExactly("first", "second", "third");
        entityService.reorder(List.of("first", "second", "third"));
        entityService.moveBefore("third", "first");

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations, org.mockito.Mockito.times(6))
                .patchUpdateItemWhere(eq(SCHEMA), eq(TABLE), body.capture(), anyMap());
        assertThat(body.getAllValues().get(0)).containsEntry("sort_order", 1);
        assertThat(body.getAllValues().get(1)).containsEntry("sort_order", 2);
        assertThat(body.getAllValues().get(2)).containsEntry("sort_order", 3);
        assertThat(body.getAllValues().get(3)).containsEntry("sort_order", 1);
        assertThat(body.getAllValues().get(4)).containsEntry("sort_order", 2);
        assertThat(body.getAllValues().get(5)).containsEntry("sort_order", 3);
    }

    @Test
    void shouldApplyTenantScopeWhenReorderingDynamicRecords() {
        IDatabaseOperations<Object> operations = operations();
        stubSortableRows(operations);
        DynamicEntityService entityService = new DynamicEntityService(new DynamicRecordDao(operations, sortableEntity()), "sales.contract");

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            entityService.reorder(List.of("first", "second", "third"));
        }

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.atLeastOnce()).query(sql.capture(), anyMap());
        assertThat(sql.getAllValues()).allSatisfy(statement -> assertThat(statement).contains("\"tenant_id\" ="));
    }

    @Test
    void shouldRejectDuplicateDynamicReorderIdsAndMissingSortField() {
        assertThatThrownBy(() -> new DynamicEntityService(new DynamicRecordDao(operations(), sortableEntity()), "sales.contract")
                .reorder(List.of("same", "same")))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("duplicate");

        assertThatThrownBy(() -> new DynamicEntityService(new DynamicRecordDao(operations(), contractEntity()), "sales.contract")
                .sortedList(Criteria.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not support capability: SORT");
    }

    @Test
    void shouldResolveDynamicReferenceTitlesAndOptions() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.row(anyString(), anyMap())).thenReturn(Map.of("total_count", 1));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(
                referenceRow("contract-1", "Contract One"),
                referenceRow("contract-2", "Contract Two")
        ));
        DynamicEntityService entityService = new DynamicEntityService(new DynamicRecordDao(operations, referenceEntity()), "sales.contract");

        assertThat(entityService.title("contract-1")).isEqualTo("Contract One");
        assertThat(entityService.titles(List.of("contract-1", "contract-2")))
                .containsEntry("contract-1", "Contract One")
                .containsEntry("contract-2", "Contract Two");
        assertThat(entityService.referenceOptions(Criteria.of(), PageRequest.of(1, 10)).getRecords())
                .containsExactly(
                        new ReferenceOption("contract-1", "Contract One"),
                        new ReferenceOption("contract-2", "Contract Two")
                );

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.atLeastOnce()).query(sql.capture(), anyMap());
        assertThat(sql.getAllValues()).anySatisfy(value -> assertThat(value)
                .contains("\"deleted\" =")
                .contains("\"deleted\" IS NULL"));
    }

    @Test
    void shouldApplyTenantScopeWhenResolvingDynamicReferences() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.row(anyString(), anyMap())).thenReturn(Map.of("total_count", 1));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(
                referenceRow("contract-1", "Contract One"),
                referenceRow("contract-2", "Contract Two")
        ));
        DynamicEntityService entityService = new DynamicEntityService(new DynamicRecordDao(operations, referenceEntity()), "sales.contract");

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            entityService.title("contract-1");
            entityService.titles(List.of("contract-1", "contract-2"));
            entityService.referenceOptions(Criteria.of(), PageRequest.of(1, 10));
        }

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.atLeastOnce()).query(sql.capture(), anyMap());
        assertThat(sql.getAllValues()).allSatisfy(statement -> assertThat(statement).contains("\"tenant_id\" ="));
    }

    @Test
    void shouldReuseTreeAbilityForDynamicRecordsWhenMetadataEnablesTree() {
        IDatabaseOperations<Object> operations = operations();
        stubTreeRows(operations);
        DynamicEntityService entityService = new DynamicEntityService(new DynamicRecordDao(operations, treeEntity()), "sales.contract");

        assertThat(entityService.children("root").stream().map(DynamicRecord::getId))
                .containsExactly("A", "B");
        assertThat(entityService.ancestorIds("A1")).containsExactly("A");
        assertThat(entityService.descendantIds("A")).containsExactly("A1");
        entityService.moveAfter("A", "B");

        assertThatThrownBy(() -> entityService.moveBefore("A1", "B"))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("same parent");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.atLeastOnce()).query(sql.capture(), anyMap());
        assertThat(sql.getAllValues()).anySatisfy(value -> assertThat(value)
                .contains("\"parent_id\" =")
                .contains("ORDER BY \"sort_order\" ASC"));

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations, org.mockito.Mockito.times(2))
                .patchUpdateItemWhere(eq(SCHEMA), eq(TABLE), body.capture(), anyMap());
        assertThat(body.getAllValues().get(0)).containsEntry("sort_order", 1);
        assertThat(body.getAllValues().get(1)).containsEntry("sort_order", 2);
    }

    @Test
    void shouldApplyTenantScopeWhenResolvingDynamicTree() {
        IDatabaseOperations<Object> operations = operations();
        stubTreeRows(operations);
        DynamicEntityService entityService = new DynamicEntityService(new DynamicRecordDao(operations, treeEntity()), "sales.contract");

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            entityService.children("root");
            entityService.ancestorIds("A1");
            entityService.descendantIds("A");
        }

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.atLeastOnce()).query(sql.capture(), anyMap());
        assertThat(sql.getAllValues()).allSatisfy(statement -> assertThat(statement).contains("\"tenant_id\" ="));
    }

    @Test
    void shouldPrepareDynamicTreeDefaultsThroughCrudAbility() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq(TABLE), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        DynamicEntityService entityService = new DynamicEntityService(new DynamicRecordDao(operations, treeEntity()), "sales.contract");
        DynamicRecord record = new DynamicRecord(treeEntity()).setValue("code", "C");
        record.setId("C");

        entityService.insert(record);

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations).insertItem(eq(SCHEMA), eq(TABLE), body.capture());
        assertThat(body.getValue()).containsEntry("parent_id", "root");
    }

    @Test
    void shouldNotPrepareAbilityDefaultsForPlainDynamicCrudFields() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq(TABLE), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        EntityDefinition entity = new EntityDefinition(
                "contract",
                TABLE,
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required(),
                        FieldDefinition.parentId(),
                        FieldDefinition.bool("enabled", "Enabled")
                )
        );
        DynamicEntityService entityService = new DynamicEntityService(new DynamicRecordDao(operations, entity), "sales.contract");
        DynamicRecord record = new DynamicRecord(entity).setValue("code", "C");
        record.setId("C");

        entityService.insert(record);

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations).insertItem(eq(SCHEMA), eq(TABLE), body.capture());
        assertThat(body.getValue()).doesNotContainKeys("parent_id", "enabled");
    }

    @Test
    void shouldRejectReferenceMethodsWithoutTitleField() {
        assertThatThrownBy(() -> new DynamicEntityService(new DynamicRecordDao(operations(), contractEntity()), "sales.contract")
                .title("contract-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not support capability: REFERENCE");
    }

    @Test
    void shouldRejectSortMethodsWithoutSortCapability() {
        assertThatThrownBy(() -> new DynamicEntityService(new DynamicRecordDao(operations(), contractEntity()), "sales.contract")
                .sortedList(Criteria.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not support capability: SORT");
    }

    @Test
    void shouldNotMutateCallerCriteria() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.row(anyString(), anyMap())).thenReturn(Map.of("total_count", 1));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of());
        DynamicRecordDao dao = dao(operations);
        Criteria criteria = Criteria.of().eq("code", "C-001");

        dao.query(criteria, PageRequest.of(1, 10));
        dao.pageQuery(criteria, PageRequest.of(1, 10));
        dao.count(criteria);

        assertThat(criteria.getClauses())
                .hasSize(1)
                .first()
                .extracting(clause -> clause.getField())
                .isEqualTo("code");
    }

    private DynamicRecordDao dao(IDatabaseOperations<Object> operations) {
        return new DynamicRecordDao(operations, contractEntity());
    }

    private DynamicEntityService entityService(IDatabaseOperations<Object> operations) {
        return new DynamicEntityService(dao(operations), "sales.contract");
    }

    @SuppressWarnings("unchecked")
    private IDatabaseOperations<Object> operations() {
        IDatabaseOperations<Object> operations = mock(IDatabaseOperations.class);
        when(operations.getDBInfo()).thenReturn(new DBInfo("POSTGRESQL").setName("muyun_test"));
        when(operations.getDefaultSchemaName()).thenReturn(SCHEMA);
        when(operations.patchUpdateItemWhere(anyString(), anyString(), anyMap(), anyMap())).thenReturn(1);
        return operations;
    }

    private EntityDefinition contractEntity() {
        return new EntityDefinition(
                "contract",
                TABLE,
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required(),
                        FieldDefinition.decimal("amount", "Amount").precision(18, 2)
                )
        );
    }

    private EntityDefinition sortableEntity() {
        return new EntityDefinition(
                "contract",
                TABLE,
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required(),
                        FieldDefinition.decimal("amount", "Amount").precision(18, 2),
                        FieldDefinition.sortOrder()
                )
        ).withCapabilities(EntityCapability.CRUD, EntityCapability.SORT);
    }

    private EntityDefinition referenceEntity() {
        return new EntityDefinition(
                "contract",
                TABLE,
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required(),
                        FieldDefinition.titleField().required(),
                        FieldDefinition.decimal("amount", "Amount").precision(18, 2)
                )
        ).withCapabilities(EntityCapability.CRUD, EntityCapability.REFERENCE);
    }

    private EntityDefinition treeEntity() {
        return new EntityDefinition(
                "contract",
                TABLE,
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required(),
                        FieldDefinition.parentId(),
                        FieldDefinition.sortOrder()
                )
        ).withCapabilities(EntityCapability.TREE);
    }

    private Map<String, Object> row(String id, int sortOrder) {
        return Map.of(
                "id", id,
                "tenant_id", "tenant-a",
                "code", id.toUpperCase(),
                "amount", BigDecimal.TEN,
                "sort_order", sortOrder,
                "deleted", Boolean.FALSE,
                "version", 0
        );
    }

    private Map<String, Object> treeRow(String id, String parentId, int sortOrder) {
        return Map.of(
                "id", id,
                "tenant_id", "tenant-a",
                "code", id,
                "parent_id", parentId,
                "sort_order", sortOrder,
                "deleted", Boolean.FALSE,
                "version", 0
        );
    }

    private Map<String, Object> referenceRow(String id, String name) {
        return Map.of(
                "id", id,
                "tenant_id", "tenant-a",
                "code", id.toUpperCase(),
                "title", name,
                "amount", BigDecimal.TEN,
                "deleted", Boolean.FALSE,
                "version", 0
        );
    }

    private void stubSortableRows(IDatabaseOperations<Object> operations) {
        when(operations.query(anyString(), anyMap())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = invocation.getArgument(1);
            String paramText = params.toString();
            if (paramText.contains("first")) {
                return List.of(row("first", 1));
            }
            if (paramText.contains("second")) {
                return List.of(row("second", 2));
            }
            if (paramText.contains("third")) {
                return List.of(row("third", 3));
            }
            return List.of(row("first", 1), row("second", 2), row("third", 3));
        });
    }

    private void stubTreeRows(IDatabaseOperations<Object> operations) {
        when(operations.query(anyString(), anyMap())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> params = invocation.getArgument(1);
            if (sql.contains("\"id\" =")) {
                if (params.containsValue("A")) {
                    return List.of(treeRow("A", "root", 1));
                }
                if (params.containsValue("B")) {
                    return List.of(treeRow("B", "root", 2));
                }
                if (params.containsValue("A1")) {
                    return List.of(treeRow("A1", "A", 1));
                }
            }
            if (sql.contains("\"parent_id\" =")) {
                if (params.containsValue("root")) {
                    return List.of(treeRow("A", "root", 1), treeRow("B", "root", 2));
                }
                if (params.containsValue("A")) {
                    return List.of(treeRow("A1", "A", 1));
                }
                return List.of();
            }
            return List.of(treeRow("A", "root", 1), treeRow("B", "root", 2), treeRow("A1", "A", 1));
        });
    }

    private static class RecordingLifecycle implements DynamicRecordLifecycle {
        private final List<String> events = new ArrayList<>();

        @Override
        public void beforeInsert(DynamicRecord record) {
            record.setValue("code", "HOOK-CODE");
            events.add("beforeInsert:" + record.getValue("code"));
        }

        @Override
        public void beforeUpdate(DynamicRecord record) {
            events.add("beforeUpdate:" + record.getVersion());
        }

        @Override
        public void beforeDelete(String id) {
            events.add("beforeDelete:" + id);
        }

        @Override
        public void afterSelect(DynamicRecord record) {
            events.add("afterSelect:" + record.getId());
        }
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
