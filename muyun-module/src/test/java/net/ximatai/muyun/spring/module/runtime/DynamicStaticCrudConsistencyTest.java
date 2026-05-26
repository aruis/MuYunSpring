package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldType;
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
        DynamicRecord record = new DynamicRecord(contractEntity())
                .setValue("code", "C-001")
                .setValue("amount", BigDecimal.TEN);
        record.setId("contract-1");

        String id = dao.insert(record);
        DynamicRecord selected = dao.findById(id);
        selected.setValue("amount", BigDecimal.ONE);
        dao.update(selected);
        dao.query(Criteria.of().eq("code", "C-001"), PageRequest.of(1, 10), Sort.desc("amount"));
        dao.count(Criteria.of().eq("code", "C-001"));
        dao.delete(id);

        ArgumentCaptor<Map<String, Object>> insertBody = mapCaptor();
        verify(operations).insertItem(eq(SCHEMA), eq(TABLE), insertBody.capture());
        assertThat(insertBody.getValue())
                .containsEntry("id", id)
                .containsEntry("version", 0)
                .containsEntry("deleted", Boolean.FALSE)
                .containsKeys("created_at", "updated_at");

        ArgumentCaptor<Map<String, Object>> patchBody = mapCaptor();
        verify(operations, org.mockito.Mockito.times(2))
                .patchUpdateItem(eq(SCHEMA), eq(TABLE), eq(id), patchBody.capture());
        assertThat(patchBody.getAllValues().get(0))
                .containsEntry("version", 1)
                .containsKey("updated_at")
                .doesNotContainKeys("created_at", "created_by");
        assertThat(patchBody.getAllValues().get(1))
                .containsEntry("deleted", Boolean.TRUE)
                .containsEntry("version", 1)
                .containsKey("updated_at");

        ArgumentCaptor<String> querySql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.atLeastOnce()).query(querySql.capture(), anyMap());
        assertThat(querySql.getAllValues()).anySatisfy(sql -> assertThat(sql)
                .contains("\"deleted\" =")
                .contains("\"deleted\" IS NULL")
                .contains("OR"));
    }

    @SuppressWarnings("unchecked")
    private IDatabaseOperations<Object> operations() {
        IDatabaseOperations<Object> operations = mock(IDatabaseOperations.class);
        when(operations.getDBInfo()).thenReturn(new DBInfo("POSTGRESQL").setName("muyun_test"));
        when(operations.getDefaultSchemaName()).thenReturn(SCHEMA);
        return operations;
    }

    private EntityDefinition contractEntity() {
        return new EntityDefinition(
                "contract",
                TABLE,
                "Contract",
                List.of(
                        new FieldDefinition("code", "code", FieldType.STRING, "Code").length(64).asRequired(),
                        new FieldDefinition("amount", "amount", FieldType.DECIMAL, "Amount").precision(18, 2)
                )
        );
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
