package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.reference.ReferenceOption;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDictionaryBinding;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                .patchUpdateItemWhere(eq(SCHEMA), eq("app_contract"), anyMap(), anyMap());
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.atLeastOnce()).query(sql.capture(), anyMap());
        assertThat(sql.getAllValues()).anySatisfy(value -> assertThat(value)
                .contains("\"deleted\" =")
                .contains("\"deleted\" IS NULL")
                .contains("ORDER BY \"amount\" DESC"));
    }

    @Test
    void shouldBindEntityOperationsForBusinessScopedCalls() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq("app_contract"), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(row("contract-1", "C-001", 0, false)));
        DynamicRecordService service = service(operations, contractEntity());
        DynamicRecordService.EntityOperations contracts = service.entity(MODULE, "contract");
        DynamicRecord record = contracts.newRecord()
                .setValue("code", "C-001")
                .setValue("amount", BigDecimal.TEN);
        record.setId("contract-1");

        assertThat(contracts.create(record)).isEqualTo("contract-1");
        assertThat(contracts.select("contract-1").getValue("code")).isEqualTo("C-001");
        contracts.delete("contract-1");

        verify(operations).insertItem(eq(SCHEMA), eq("app_contract"), anyMap());
        verify(operations).patchUpdateItemWhere(eq(SCHEMA), eq("app_contract"), anyMap(), anyMap());
    }

    @Test
    void shouldKeepCapabilityGatesThroughBoundEntityOperations() {
        DynamicRecordService service = service(operations(), contractEntity());
        DynamicRecordService.EntityOperations contracts = service.entity(MODULE, "contract");

        assertThatThrownBy(() -> contracts.enable("contract-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ENABLE");
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
                .patchUpdateItemWhere(eq(SCHEMA), eq("app_contract"), body.capture(), anyMap());
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
        assertThat(service.projections(MODULE, "contract", List.of("contract-2", "contract-1"), List.of("code", "title")))
                .containsExactly(
                        Map.entry("contract-2", Map.of("code", "CONTRACT-2", "title", "Contract Two")),
                        Map.entry("contract-1", Map.of("code", "CONTRACT-1", "title", "Contract One"))
                );
        assertThat(service.referenceOptions(MODULE, "contract", Criteria.of(), PageRequest.of(1, 10)).getRecords())
                .containsExactly(
                        new ReferenceOption("contract-1", "Contract One"),
                        new ReferenceOption("contract-2", "Contract Two")
                );
    }

    @Test
    void shouldResolveDynamicReferenceQueryThroughStableServiceApi() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.row(anyString(), anyMap())).thenReturn(Map.of("total_count", 2));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(
                referenceRow("contract-1", "Contract One"),
                referenceRow("contract-2", "Contract Two")
        ));
        DynamicRecordService service = referenceResolvingService(operations);

        DynamicReferenceResolveResponse response = service.resolveReference(
                MODULE,
                "line",
                "contractId",
                DynamicReferenceResolveRequest.query("Contract")
        );

        assertThat(response.status()).isEqualTo(DynamicReferenceResolveStatus.OK);
        assertThat(response.mode()).isEqualTo(DynamicReferenceResolveMode.QUERY);
        assertThat(response.total()).isEqualTo(2);
        assertThat(response.options()).extracting(DynamicReferenceResolveItem::id)
                .containsExactly("contract-1", "contract-2");
        assertThat(response.options().getFirst().title()).isEqualTo("Contract One");
        assertThat(response.options().getFirst().projections()).containsEntry("contractCode", "CONTRACT-1");
    }

    @Test
    void shouldTranslateDynamicReferenceValuesThroughStableServiceApi() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.row(anyString(), anyMap())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> params = invocation.getArgument(1);
            if (params.containsValue("contract-1") && sql.contains("\"id\"")) {
                return Map.of("total_count", 1);
            }
            if (params.containsValue("Contract Two") && sql.contains("\"title\"")) {
                return Map.of("total_count", 1);
            }
            return Map.of("total_count", 0);
        });
        when(operations.query(anyString(), anyMap())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> params = invocation.getArgument(1);
            if (params.containsValue("contract-1") && sql.contains("\"id\"")) {
                return List.of(referenceRow("contract-1", "Contract One"));
            }
            if (params.containsValue("Contract Two") && sql.contains("\"title\"")) {
                return List.of(referenceRow("contract-2", "Contract Two"));
            }
            return List.of();
        });
        DynamicRecordService service = referenceResolvingService(operations);

        DynamicReferenceResolveResponse response = service.resolveReference(
                MODULE,
                "line",
                "contractId",
                DynamicReferenceResolveRequest.translate(List.of("contract-1", "Contract Two", "missing"))
        );

        assertThat(response.status()).isEqualTo(DynamicReferenceResolveStatus.PARTIAL);
        assertThat(response.results()).extracting(DynamicReferenceResolveResult::status)
                .containsExactly(DynamicReferenceResolveStatus.RESOLVED,
                        DynamicReferenceResolveStatus.RESOLVED,
                        DynamicReferenceResolveStatus.NOT_FOUND);
        assertThat(response.results().getFirst().matchedBy()).isEqualTo(DynamicReferenceMatchMode.KEY);
        assertThat(response.results().get(1).matchedBy()).isEqualTo(DynamicReferenceMatchMode.LABEL);
        assertThat(response.results().getFirst().item().projections()).containsEntry("contractCode", "CONTRACT-1");
    }

    @Test
    void shouldOmitReferenceProjectionsWhenRequested() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.row(anyString(), anyMap())).thenReturn(Map.of("total_count", 1));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(referenceRow("contract-1", "Contract One")));
        DynamicRecordService service = referenceResolvingService(operations);

        DynamicReferenceResolveResponse response = service.resolveReference(
                MODULE,
                "line",
                "contractId",
                DynamicReferenceResolveRequest.query("Contract").withoutProjections()
        );

        assertThat(response.options().getFirst().projections()).isEmpty();
    }

    @Test
    void shouldReportLabelAmbiguityWhenTranslatingReference() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.row(anyString(), anyMap())).thenReturn(Map.of("total_count", 2));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(
                referenceRow("contract-1", "Contract"),
                referenceRow("contract-2", "Contract")
        ));
        DynamicRecordService service = referenceResolvingService(operations);

        DynamicReferenceResolveResponse response = service.resolveReference(
                MODULE,
                "line",
                "contractId",
                DynamicReferenceResolveRequest.translate(List.of("Contract"))
                        .withMatchMode(DynamicReferenceMatchMode.LABEL)
        );

        assertThat(response.status()).isEqualTo(DynamicReferenceResolveStatus.AMBIGUOUS);
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().getFirst().status()).isEqualTo(DynamicReferenceResolveStatus.AMBIGUOUS);
        assertThat(response.results().getFirst().matchedBy()).isEqualTo(DynamicReferenceMatchMode.LABEL);
        assertThat(response.results().getFirst().candidates()).extracting(DynamicReferenceResolveItem::id)
                .containsExactly("contract-1", "contract-2");
    }

    @Test
    void shouldPreserveKeyAmbiguityWhenAutoTranslateWouldResolveByLabel() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.row(anyString(), anyMap())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> params = invocation.getArgument(1);
            if (params.containsValue("shared") && sql.contains("\"id\"")) {
                return Map.of("total_count", 2);
            }
            if (params.containsValue("shared") && sql.contains("\"title\"")) {
                return Map.of("total_count", 1);
            }
            return Map.of("total_count", 0);
        });
        when(operations.query(anyString(), anyMap())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> params = invocation.getArgument(1);
            if (params.containsValue("shared") && sql.contains("\"id\"")) {
                return List.of(
                        referenceRow("shared", "First"),
                        referenceRow("shared", "Second")
                );
            }
            if (params.containsValue("shared") && sql.contains("\"title\"")) {
                return List.of(referenceRow("contract-3", "shared"));
            }
            return List.of();
        });
        DynamicRecordService service = referenceResolvingService(operations);

        DynamicReferenceResolveResponse response = service.resolveReference(
                MODULE,
                "line",
                "contractId",
                DynamicReferenceResolveRequest.translate(List.of("shared"))
        );

        assertThat(response.status()).isEqualTo(DynamicReferenceResolveStatus.AMBIGUOUS);
        assertThat(response.results().getFirst().matchedBy()).isEqualTo(DynamicReferenceMatchMode.KEY);
        assertThat(response.results().getFirst().candidates()).hasSize(2);
        verify(operations, org.mockito.Mockito.never()).row(org.mockito.ArgumentMatchers.contains("\"title\""), anyMap());
    }

    @Test
    void shouldApplyCriteriaWhenResolvingReference() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.row(anyString(), anyMap())).thenReturn(Map.of("total_count", 1));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(referenceRow("contract-1", "Contract One")));
        DynamicRecordService service = referenceResolvingService(operations);

        DynamicReferenceResolveResponse response = service.resolveReference(
                MODULE,
                "line",
                "contractId",
                DynamicReferenceResolveRequest.query("Contract")
                        .withCriteria(Criteria.of().eq("code", "CONTRACT-1"))
        );

        assertThat(response.status()).isEqualTo(DynamicReferenceResolveStatus.OK);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.atLeastOnce()).query(sql.capture(), anyMap());
        assertThat(sql.getAllValues()).anySatisfy(statement -> assertThat(statement)
                .contains("\"code\" =")
                .contains("\"title\" LIKE"));
    }

    @Test
    void shouldReturnNotFoundForEmptyReferenceTranslateValues() {
        DynamicRecordService service = referenceResolvingService(operations());

        DynamicReferenceResolveResponse response = service.resolveReference(
                MODULE,
                "line",
                "contractId",
                DynamicReferenceResolveRequest.translate(List.of())
        );

        assertThat(response.status()).isEqualTo(DynamicReferenceResolveStatus.NOT_FOUND);
        assertThat(response.results()).isEmpty();
        assertThat(response.total()).isZero();
    }

    @Test
    void shouldRejectMissingDynamicReferenceConfig() {
        DynamicRecordService service = service(operations(), lineEntity());

        assertThatThrownBy(() -> service.resolveReference(
                MODULE,
                "line",
                "contractId",
                DynamicReferenceResolveRequest.query("Contract")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dynamic reference is not configured: line.contractId");
    }

    @Test
    void shouldExposeEnableOperationsThroughStableServiceApi() {
        IDatabaseOperations<Object> operations = operations();
        AtomicReference<Boolean> enabled = new AtomicReference<>(Boolean.FALSE);
        when(operations.query(anyString(), anyMap()))
                .thenAnswer(invocation -> List.of(enabledRow("contract-1", enabled.get())));
        when(operations.patchUpdateItemWhere(anyString(), anyString(), anyMap(), anyMap()))
                .thenAnswer(invocation -> {
                    enabled.set((Boolean) invocation.<Map<String, Object>>getArgument(2).get("enabled"));
                    return 1;
                });
        DynamicRecordService service = service(operations, enabledEntity());

        assertThat(service.isEnabled(MODULE, "contract", "contract-1")).isFalse();
        assertThat(service.enable(MODULE, "contract", "contract-1")).isEqualTo(1);
        assertThat(service.isEnabled(MODULE, "contract", "contract-1")).isTrue();
        assertThat(service.disable(MODULE, "contract", "contract-1")).isEqualTo(1);
        assertThat(service.isEnabled(MODULE, "contract", "contract-1")).isFalse();
        Criteria activeContracts = service.enabledCriteria(MODULE, "contract", Criteria.of().eq("code", "CONTRACT-1"));
        service.list(MODULE, "contract", activeContracts, PageRequest.of(1, 10));

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations, org.mockito.Mockito.times(2))
                .patchUpdateItemWhere(eq(SCHEMA), eq("app_contract"), body.capture(), anyMap());
        assertThat(body.getAllValues().get(0)).containsEntry("enabled", Boolean.TRUE);
        assertThat(body.getAllValues().get(1)).containsEntry("enabled", Boolean.FALSE);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.atLeastOnce()).query(sql.capture(), anyMap());
        assertThat(sql.getAllValues()).anySatisfy(statement -> assertThat(statement)
                .contains("\"enabled\" =")
                .contains("\"code\" ="));
    }

    @Test
    void shouldRejectUnsupportedStableServiceAbilityApi() {
        DynamicRecordService service = service(operations(), contractEntity());

        assertThatThrownBy(() -> service.enable(MODULE, "contract", "contract-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ENABLE");
        assertThatThrownBy(() -> service.disable(MODULE, "contract", "contract-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ENABLE");
        assertThatThrownBy(() -> service.isEnabled(MODULE, "contract", "contract-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ENABLE");
        assertThatThrownBy(() -> service.enabledCriteria(MODULE, "contract", Criteria.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ENABLE");
        assertThatThrownBy(() -> service.projections(MODULE, "contract", List.of("contract-1"), List.of("code")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REFERENCE");
    }

    @Test
    void shouldValidateDictionaryBoundFieldThroughRuntimeValidator() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq("app_contract"), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        DynamicFieldValueValidator validator = (moduleAlias, entity, field, value) -> {
            FieldDictionaryBinding binding = field.dictionaryBinding();
            if (binding != null && !"active".equals(value)) {
                throw new IllegalArgumentException("invalid dictionary code: " + value);
            }
        };
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations, validator)
                .register(new ModuleDefinition(MODULE, "Contract", List.of(dictionaryEntity())));
        DynamicRecordService.EntityOperations contracts = new DynamicRecordService(runtime).entity(MODULE, "contract");
        DynamicRecord record = contracts.newRecord()
                .setValue("code", "C-001")
                .setValue("status", "active");
        record.setId("contract-1");

        assertThat(contracts.create(record)).isEqualTo("contract-1");
        DynamicRecord invalid = contracts.newRecord()
                .setValue("code", "C-002")
                .setValue("status", "frozen");
        invalid.setId("contract-2");
        assertThatThrownBy(() -> contracts.create(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dictionary code");
    }

    @Test
    void shouldCacheDynamicSelectAcrossRuntimeFacadeCallsAndClearAfterUpdate() {
        IDatabaseOperations<Object> operations = operations();
        AtomicReference<String> storedCode = new AtomicReference<>("C-001");
        when(operations.query(anyString(), anyMap()))
                .thenAnswer(invocation -> List.of(row("contract-1", storedCode.get(), 0, false)));
        when(operations.patchUpdateItemWhere(anyString(), anyString(), anyMap(), anyMap()))
                .thenAnswer(invocation -> {
                    storedCode.set(String.valueOf(invocation.<Map<String, Object>>getArgument(2).get("code")));
                    return 1;
                });
        DynamicRecordService service = service(operations, contractEntity());

        DynamicRecord first = service.select(MODULE, "contract", "contract-1");
        first.setValue("code", "MUTATED-CALLER");
        DynamicRecord second = service.select(MODULE, "contract", "contract-1");
        assertThat(second.getValue("code")).isEqualTo("C-001");

        second.setValue("code", "C-002");
        service.update(MODULE, "contract", second);
        DynamicRecord third = service.select(MODULE, "contract", "contract-1");

        assertThat(third.getValue("code")).isEqualTo("C-002");
        verify(operations, org.mockito.Mockito.times(3)).query(anyString(), anyMap());
    }

    @SuppressWarnings("unchecked")
    private IDatabaseOperations<Object> operations() {
        IDatabaseOperations<Object> operations = mock(IDatabaseOperations.class);
        when(operations.getDBInfo()).thenReturn(new DBInfo("POSTGRESQL").setName("muyun_test"));
        when(operations.getDefaultSchemaName()).thenReturn(SCHEMA);
        when(operations.patchUpdateItemWhere(anyString(), anyString(), anyMap(), anyMap())).thenReturn(1);
        return operations;
    }

    private DynamicRecordService service(IDatabaseOperations<Object> operations, EntityDefinition entity) {
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations)
                .register(new ModuleDefinition(MODULE, "Contract", List.of(entity)));
        return new DynamicRecordService(runtime);
    }

    private DynamicRecordService referenceResolvingService(IDatabaseOperations<Object> operations) {
        ModuleDefinition module = new ModuleDefinition(
                MODULE,
                "Contract",
                List.of(referenceEntity(), lineEntity()),
                List.of(),
                List.of(EntityReferenceDefinition.to("line", "contractId", ReferenceTarget.of(MODULE, "contract"))
                        .withAutoTitle("contractTitle")
                        .withProjection("code", "contractCode"))
        );
        return new DynamicRecordService(new DynamicRecordRuntime(operations).register(module));
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

    private EntityDefinition dictionaryEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required(),
                        FieldDefinition.string("status", "Status").dictionary("crm", "customer_status")
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

    private EntityDefinition lineEntity() {
        return new EntityDefinition(
                "line",
                "app_contract_line",
                "Contract Line",
                List.of(
                        FieldDefinition.string("contractId", "Contract").column("contract_id").length(32),
                        FieldDefinition.string("summary", "Summary").length(128)
                )
        );
    }

    private EntityDefinition enabledEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required(),
                        FieldDefinition.enabled()
                )
        ).withCapabilities(EntityCapability.CRUD, EntityCapability.ENABLE);
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

    private Map<String, Object> enabledRow(String id, boolean enabled) {
        return Map.of(
                "id", id,
                "code", id.toUpperCase(),
                "enabled", enabled,
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
