package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.database.core.orm.SqlRawCondition;
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

class DynamicRecordDaoTest {
    private static final String SCHEMA = "public";
    private static final String TABLE = "app_contract";

    @Test
    void shouldInsertWithTableColumnsAndBaseModelDefaults() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq(TABLE), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        DynamicRecord record = new DynamicRecord(contractEntity())
                .setValue("code", "C-001")
                .setValue("amount", BigDecimal.TEN);

        String id = dao(operations).insert(record);

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations).insertItem(eq(SCHEMA), eq(TABLE), body.capture());
        assertThat(id).hasSize(32);
        assertThat(body.getValue())
                .containsEntry("id", id)
                .containsEntry("code", "C-001")
                .containsEntry("amount", BigDecimal.TEN)
                .containsEntry("version", 0)
                .containsEntry("deleted", Boolean.FALSE);
        assertThat(body.getValue()).containsKeys("created_at", "updated_at");
    }

    @Test
    void shouldUpdateByTableColumnsAndIncrementVersion() {
        IDatabaseOperations<Object> operations = operations();
        DynamicRecord record = new DynamicRecord(contractEntity())
                .setValue("code", "C-001")
                .setValue("amount", BigDecimal.TEN);
        record.setId("contract-1");
        record.setVersion(3);

        dao(operations).update(record);

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations).patchUpdateItem(eq(SCHEMA), eq(TABLE), eq("contract-1"), body.capture());
        assertThat(body.getValue())
                .containsEntry("code", "C-001")
                .containsEntry("amount", BigDecimal.TEN)
                .containsEntry("version", 4);
        assertThat(body.getValue())
                .containsKey("updated_at")
                .doesNotContainKeys("id", "created_at", "created_by");
    }

    @Test
    void shouldResolveCurrentVersionWhenUpdatingPartialRecord() {
        IDatabaseOperations<Object> operations = operations();
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

        dao(operations).update(record);

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations).patchUpdateItem(eq(SCHEMA), eq(TABLE), eq("contract-1"), body.capture());
        assertThat(body.getValue())
                .containsEntry("amount", BigDecimal.ONE)
                .containsEntry("version", 8)
                .doesNotContainKeys("created_at", "created_by", "code");
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

        dao(operations).delete("contract-1");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations).query(sql.capture(), anyMap());
        assertThat(sql.getValue())
                .contains("\"id\" =")
                .contains("\"deleted\" =")
                .contains("\"deleted\" IS NULL")
                .contains("OR");
        assertThat(occurrences(sql.getValue(), "\"deleted\" =")).isEqualTo(1);

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations).patchUpdateItem(eq(SCHEMA), eq(TABLE), eq("contract-1"), body.capture());
        assertThat(body.getValue())
                .containsEntry("deleted", Boolean.TRUE)
                .containsEntry("version", 3);
        assertThat(body.getValue()).doesNotContainKeys("id", "code", "amount", "created_at", "created_by");
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

        assertThat(dao.query(Criteria.of().eq("code", "C-001"), PageRequest.of(1, 10), Sort.desc("amount")))
                .hasSize(1)
                .first()
                .extracting(record -> record.getValue("code"))
                .isEqualTo("C-001");
        assertThat(dao.page(Criteria.of().eq("code", "C-001"), PageRequest.of(1, 10)).getTotal())
                .isEqualTo(1);
        assertThat(dao.count(Criteria.of().eq("code", "C-001"))).isEqualTo(1);

        ArgumentCaptor<String> querySql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.times(2)).query(querySql.capture(), anyMap());
        assertThat(querySql.getAllValues().getFirst())
                .contains("FROM \"public\".\"app_contract\"")
                .contains("\"code\" =")
                .contains("\"deleted\" =")
                .contains("\"deleted\" IS NULL")
                .contains("ORDER BY \"amount\" DESC")
                .contains("LIMIT :limit OFFSET :offset");

        ArgumentCaptor<String> countSql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.times(2)).row(countSql.capture(), anyMap());
        assertThat(countSql.getAllValues().getFirst())
                .startsWith("SELECT COUNT(*) AS total_count FROM \"public\".\"app_contract\"")
                .contains("\"code\" =")
                .contains("\"deleted\" =");
    }

    private DynamicRecordDao dao(IDatabaseOperations<Object> operations) {
        return new DynamicRecordDao(operations, contractEntity());
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

    private int occurrences(String value, String part) {
        return value.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
