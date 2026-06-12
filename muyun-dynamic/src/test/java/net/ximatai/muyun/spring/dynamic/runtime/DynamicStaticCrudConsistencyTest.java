package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.identity.ActingContext;
import net.ximatai.muyun.spring.common.identity.ActingContextHolder;
import net.ximatai.muyun.spring.common.identity.BusinessPrincipal;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicStaticCrudConsistencyTest {
    private static final String SCHEMA = "public";
    private static final String TABLE = "app_contract";

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        ActingContextHolder.clear();
    }

    @Test
    void shouldMatchStaticCrudDefaultsForInsertUpdateSoftDeleteAndActiveQuery() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq(TABLE), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(Map.of(
                "id", "contract-1",
                "code", "C-001",
                "amount", BigDecimal.TEN,
                "deleted", Boolean.FALSE,
                "version", 0
        )));
        when(operations.row(anyString(), anyMap())).thenReturn(Map.of("total_count", 1));

        DynamicRecordDao dao = new DynamicRecordDao(operations, contractEntity());
        DynamicEntityService entityService = new DynamicEntityService(dao, "sales.contract");
        DynamicRecord record = new DynamicRecord(contractEntity())
                .setValue("code", "C-001")
                .setValue("amount", BigDecimal.TEN);
        record.setId("contract-1");
        record.setTenantId("tenant-a");

        String id;
        try (CurrentUserContext.Scope ignored = CurrentUserContext.use(
                CurrentUser.tenantUser("operator-1", "Operator", "tenant-a"))) {
            id = entityService.insert(record);
            DynamicRecord selected = entityService.select(id);
            selected.setValue("amount", BigDecimal.ONE);
            entityService.update(selected);
            entityService.pageQuery(Criteria.of().eq("code", "C-001"), PageRequest.of(1, 10), Sort.desc("amount"));
            entityService.count(Criteria.of().eq("code", "C-001"));
            entityService.delete(id);
        }

        ArgumentCaptor<Map<String, Object>> insertBody = mapCaptor();
        verify(operations).insertItem(eq(SCHEMA), eq(TABLE), insertBody.capture());
        assertThat(insertBody.getValue())
                .containsEntry("id", id)
                .containsEntry("tenant_id", "tenant-a")
                .containsEntry("version", 0)
                .containsEntry("deleted", Boolean.FALSE)
                .containsEntry("deleted_at", null)
                .containsEntry("created_by", "operator-1")
                .containsEntry("updated_by", "operator-1")
                .containsKeys("created_at", "updated_at");

        ArgumentCaptor<Map<String, Object>> patchBody = mapCaptor();
        verify(operations, org.mockito.Mockito.times(2))
                .patchUpdateItemWhere(eq(SCHEMA), eq(TABLE), patchBody.capture(), anyMap());
        assertThat(patchBody.getAllValues().get(0))
                .containsEntry("version", 1)
                .containsEntry("updated_by", "operator-1")
                .containsKey("updated_at")
                .doesNotContainKeys("created_at", "created_by");
        assertThat(patchBody.getAllValues().get(1))
                .containsEntry("deleted", Boolean.TRUE)
                .containsEntry("version", 1)
                .containsEntry("updated_by", "operator-1")
                .containsKeys("deleted_at", "updated_at");

        ArgumentCaptor<String> querySql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.atLeastOnce()).query(querySql.capture(), anyMap());
        assertThat(querySql.getAllValues()).anySatisfy(sql -> assertThat(sql)
                .contains("\"deleted\" =")
                .contains("\"deleted\" IS NULL")
                .contains("OR"));
    }

    @Test
    void shouldKeepDynamicAuditOperatorAsActualUserWhenActing() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq(TABLE), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        DynamicEntityService entityService = new DynamicEntityService(new DynamicRecordDao(operations, contractEntity()),
                "sales.contract");
        DynamicRecord record = new DynamicRecord(contractEntity())
                .setValue("code", "C-001")
                .setValue("amount", BigDecimal.TEN);
        CurrentUser operator = CurrentUser.tenantUser("assistant-user", "Assistant", "tenant-a");
        BusinessPrincipal principal = BusinessPrincipal.employee(
                "employee-principal", "org-principal", "dept-principal");

        try (CurrentUserContext.Scope user = CurrentUserContext.use(operator);
             ActingContextHolder.Scope acting = ActingContextHolder.use(new ActingContext(
                     "delegation-1", operator, principal, "sales.contract", "create"))) {
            entityService.insert(record);
        }

        ArgumentCaptor<Map<String, Object>> insertBody = mapCaptor();
        verify(operations).insertItem(eq(SCHEMA), eq(TABLE), insertBody.capture());
        assertThat(insertBody.getValue())
                .containsEntry("created_by", "assistant-user")
                .containsEntry("updated_by", "assistant-user");
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

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
