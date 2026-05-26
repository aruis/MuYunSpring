package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.ReferenceOption;
import net.ximatai.muyun.spring.module.metadata.EntityCapability;
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

class DynamicRecordServiceTest {
    private static final String SCHEMA = "public";
    private static final String MODULE = "sales.contract";

    @Test
    void shouldRunCrudThroughStableServiceApi() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq("app_contract"), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(row("contract-1", "C-001", 0, false)));
        when(operations.row(anyString(), anyMap())).thenReturn(Map.of("total_count", 1));
        DynamicRecordService service = service(operations, contractEntity());
        DynamicRecord record = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001")
                .setValue("amount", BigDecimal.TEN);
        record.setId("contract-1");

        String id = service.create(MODULE, "contract", record);
        DynamicRecord selected = service.select(MODULE, "contract", id);
        selected.setValue("amount", BigDecimal.ONE);
        service.update(MODULE, "contract", selected);
        assertThat(service.list(MODULE, "contract", Criteria.of().eq("code", "C-001"), PageRequest.of(1, 10), Sort.desc("amount")))
                .hasSize(1);
        service.page(MODULE, "contract", Criteria.of().eq("code", "C-001"), PageRequest.of(1, 10));
        assertThat(service.count(MODULE, "contract", Criteria.of().eq("code", "C-001"))).isEqualTo(1);
        service.delete(MODULE, "contract", id);
        assertThat(service.selectIgnoreSoftDelete(MODULE, "contract", id)).isNotNull();
        service.deleteBatch(MODULE, "contract", List.of(id));

        verify(operations).insertItem(eq(SCHEMA), eq("app_contract"), anyMap());
        verify(operations, org.mockito.Mockito.times(3))
                .patchUpdateItem(eq(SCHEMA), eq("app_contract"), eq(id), anyMap());
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.atLeastOnce()).query(sql.capture(), anyMap());
        assertThat(sql.getAllValues()).anySatisfy(value -> assertThat(value)
                .contains("\"deleted\" =")
                .contains("\"deleted\" IS NULL")
                .contains("ORDER BY \"amount\" DESC"));
    }

    @Test
    void shouldExposeSortOperationsThroughStableServiceApi() {
        IDatabaseOperations<Object> operations = operations();
        stubSortableRows(operations);
        DynamicRecordService service = service(operations, sortableEntity());

        assertThat(service.sortedList(MODULE, "contract", Criteria.of()).stream().map(DynamicRecord::getId))
                .containsExactly("first", "second", "third");
        service.reorder(MODULE, "contract", List.of("first", "second", "third"));

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations, org.mockito.Mockito.times(3))
                .patchUpdateItem(eq(SCHEMA), eq("app_contract"), anyString(), body.capture());
        assertThat(body.getAllValues().get(0)).containsEntry("sort_order", 1);
        assertThat(body.getAllValues().get(1)).containsEntry("sort_order", 2);
        assertThat(body.getAllValues().get(2)).containsEntry("sort_order", 3);
    }

    @Test
    void shouldExposeReferenceOperationsThroughStableServiceApi() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.row(anyString(), anyMap())).thenReturn(Map.of("total_count", 2));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(
                referenceRow("contract-1", "Contract One"),
                referenceRow("contract-2", "Contract Two")
        ));
        DynamicRecordService service = service(operations, referenceEntity());

        assertThat(service.title(MODULE, "contract", "contract-1")).isEqualTo("Contract One");
        assertThat(service.titles(MODULE, "contract", List.of("contract-1", "contract-2")))
                .containsEntry("contract-1", "Contract One")
                .containsEntry("contract-2", "Contract Two");
        assertThat(service.referenceOptions(MODULE, "contract", Criteria.of(), PageRequest.of(1, 10)).getRecords())
                .containsExactly(
                        new ReferenceOption("contract-1", "Contract One"),
                        new ReferenceOption("contract-2", "Contract Two")
                );
    }

    @SuppressWarnings("unchecked")
    private IDatabaseOperations<Object> operations() {
        IDatabaseOperations<Object> operations = mock(IDatabaseOperations.class);
        when(operations.getDBInfo()).thenReturn(new DBInfo("POSTGRESQL").setName("muyun_test"));
        when(operations.getDefaultSchemaName()).thenReturn(SCHEMA);
        return operations;
    }

    private DynamicRecordService service(IDatabaseOperations<Object> operations, EntityDefinition entity) {
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations)
                .register(new ModuleDefinition(MODULE, "Contract", List.of(entity)));
        return new DynamicRecordService(runtime);
    }

    private EntityDefinition contractEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
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
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required(),
                        FieldDefinition.sortOrder()
                )
        ).withCapabilities(EntityCapability.CRUD, EntityCapability.SORT);
    }

    private EntityDefinition referenceEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required(),
                        FieldDefinition.titleField().required()
                )
        ).withCapabilities(EntityCapability.CRUD, EntityCapability.REFERENCE);
    }

    private Map<String, Object> row(String id, String code, int version, boolean deleted) {
        return Map.of(
                "id", id,
                "code", code,
                "amount", BigDecimal.TEN,
                "deleted", deleted,
                "version", version
        );
    }

    private Map<String, Object> sortableRow(String id, int sortOrder) {
        return Map.of(
                "id", id,
                "code", id.toUpperCase(),
                "sort_order", sortOrder,
                "deleted", Boolean.FALSE,
                "version", 0
        );
    }

    private Map<String, Object> referenceRow(String id, String name) {
        return Map.of(
                "id", id,
                "code", id.toUpperCase(),
                "title", name,
                "deleted", Boolean.FALSE,
                "version", 0
        );
    }

    private void stubSortableRows(IDatabaseOperations<Object> operations) {
        when(operations.query(anyString(), anyMap())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = invocation.getArgument(1);
            if (params.containsValue("first")) {
                return List.of(sortableRow("first", 1));
            }
            if (params.containsValue("second")) {
                return List.of(sortableRow("second", 2));
            }
            if (params.containsValue("third")) {
                return List.of(sortableRow("third", 3));
            }
            return List.of(
                    sortableRow("first", 1),
                    sortableRow("second", 2),
                    sortableRow("third", 3)
            );
        });
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return ArgumentCaptor.forClass(Map.class);
    }
}
