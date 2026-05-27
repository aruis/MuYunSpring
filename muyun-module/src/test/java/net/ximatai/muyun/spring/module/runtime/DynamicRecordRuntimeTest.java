package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinition;
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

class DynamicRecordRuntimeTest {
    private static final String SCHEMA = "public";
    private static final String TABLE = "app_contract";

    @Test
    void shouldCreateEntityServiceFromRegisteredModule() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq(TABLE), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations)
                .register(contractModule());

        DynamicEntityService entityService = runtime.entityService("sales.contract", "contract");
        DynamicRecord record = runtime.newRecord("sales.contract", "contract")
                .setValue("code", "C-001")
                .setValue("amount", BigDecimal.TEN);

        String id = entityService.insert(record);

        assertThat(id).hasSize(32);
        assertThat(entityService.getModuleAlias()).isEqualTo("sales.contract");
        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations).insertItem(eq(SCHEMA), eq(TABLE), body.capture());
        assertThat(body.getValue())
                .containsEntry("id", id)
                .containsEntry("code", "C-001")
                .containsEntry("amount", BigDecimal.TEN)
                .containsEntry("version", 0)
                .containsEntry("deleted", Boolean.FALSE);
    }

    @Test
    void shouldRunDynamicCrudThroughRuntimeServiceChain() {
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
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations)
                .register(contractModule());
        DynamicEntityService entityService = runtime.entityService("sales.contract", "contract");
        DynamicRecord record = runtime.newRecord("sales.contract", "contract")
                .setValue("code", "C-001")
                .setValue("amount", BigDecimal.TEN);
        record.setId("contract-1");

        String id = entityService.insert(record);
        DynamicRecord selected = entityService.select(id);
        selected.setValue("amount", BigDecimal.ONE);
        entityService.update(selected);
        entityService.pageQuery(Criteria.of().eq("code", "C-001"), PageRequest.of(1, 10));
        entityService.delete(id);

        verify(operations).insertItem(eq(SCHEMA), eq(TABLE), anyMap());
        verify(operations, org.mockito.Mockito.times(2))
                .patchUpdateItemWhere(eq(SCHEMA), eq(TABLE), anyMap(), anyMap());
        ArgumentCaptor<String> querySql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.atLeastOnce()).query(querySql.capture(), anyMap());
        assertThat(querySql.getAllValues()).anySatisfy(sql -> assertThat(sql)
                .contains("\"deleted\" =")
                .contains("\"deleted\" IS NULL"));
    }

    @SuppressWarnings("unchecked")
    private IDatabaseOperations<Object> operations() {
        IDatabaseOperations<Object> operations = mock(IDatabaseOperations.class);
        when(operations.getDBInfo()).thenReturn(new DBInfo("POSTGRESQL").setName("muyun_test"));
        when(operations.getDefaultSchemaName()).thenReturn(SCHEMA);
        when(operations.patchUpdateItemWhere(anyString(), anyString(), anyMap(), anyMap())).thenReturn(1);
        return operations;
    }

    private ModuleDefinition contractModule() {
        return new ModuleDefinition("sales.contract", "Contract", List.of(contractEntity()));
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
