package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.event.RuntimeEvent;
import net.ximatai.muyun.spring.ability.event.RuntimeEventPublisher;
import net.ximatai.muyun.spring.ability.event.RuntimeEventType;
import net.ximatai.muyun.spring.ability.event.RuntimeMutationSource;
import net.ximatai.muyun.spring.ability.reference.ReferenceOption;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.common.formula.FormulaRulePhase;
import net.ximatai.muyun.spring.common.formula.FormulaIssueLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionKind;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionStyle;
import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityFormulaRuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDictionaryBinding;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
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
    void shouldEvaluateConfiguredActionAvailabilityThroughStableServiceApi() {
        DynamicRecordService service = actionService(operations());
        DynamicRecord draft = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001")
                .setValue("status", "draft");
        DynamicRecord submitted = service.newRecord(MODULE, "contract")
                .setValue("code", "C-002")
                .setValue("status", "submitted");

        DynamicActionAvailability available = service.actionAvailability(MODULE, "submit", draft);
        DynamicActionAvailability unavailable = service.entity(MODULE, "contract")
                .actionAvailability("submit", submitted);

        assertThat(service.action(MODULE, "submit").availabilityCondition()).isTrue();
        assertThat(available.available()).isTrue();
        assertThat(available.report().errors()).isEmpty();
        assertThat(unavailable.available()).isFalse();
        assertThat(unavailable.message()).isEqualTo("只有草稿合同可以提交");
        assertThat(unavailable.report().errors()).singleElement()
                .extracting(error -> error.phase())
                .isEqualTo(net.ximatai.muyun.spring.common.formula.FormulaRulePhase.ACTION_AVAILABLE);
        assertThat(service.module(MODULE).actionAvailability("submit", draft).available()).isTrue();
    }

    @Test
    void shouldExposeModuleActionsAsMainEntityActions() {
        DynamicRecordService service = actionService(operations());

        assertThat(service.module(MODULE).actions())
                .containsExactlyElementsOf(service.entity(MODULE, "contract").actions());
        assertThat(service.module(MODULE).action("submit"))
                .isEqualTo(service.entity(MODULE, "contract").action("submit"));
    }

    @Test
    void shouldEvaluateModuleActionAvailabilityAsMainEntityActionAvailability() {
        DynamicRecordService service = actionService(operations());
        DynamicRecord draft = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001")
                .setValue("status", "draft");

        DynamicActionAvailability moduleAvailability = service.module(MODULE).actionAvailability("submit", draft);
        DynamicActionAvailability entityAvailability = service.entity(MODULE, "contract").actionAvailability("submit", draft);

        assertThat(moduleAvailability.available()).isEqualTo(entityAvailability.available());
        assertThat(moduleAvailability.message()).isEqualTo(entityAvailability.message());
        assertThat(moduleAvailability.report().errors()).hasSameSizeAs(entityAvailability.report().errors());
    }

    @Test
    void shouldTreatStandardActionWithoutConditionAsAvailable() {
        DynamicRecordService service = actionService(operations());
        DynamicRecord record = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001");

        assertThat(service.entity(MODULE, "contract").actionAvailability("create", record).available()).isTrue();
    }

    @Test
    void shouldExecuteStandardCreateActionThroughStableActionApi() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq("app_contract"), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        DynamicRecordService service = actionService(operations);
        DynamicRecord record = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001")
                .setValue("status", "draft");
        record.setId("contract-1");

        DynamicActionExecutionResult result = service.module(MODULE)
                .executeAction("create", DynamicActionExecutionRequest.record(record));

        assertThat(result.value()).isEqualTo("contract-1");
        assertThat(result.context().moduleAlias()).isEqualTo(MODULE);
        assertThat(result.context().entityAlias()).isEqualTo("contract");
        assertThat(result.context().actionCode()).isEqualTo("create");
        assertThat(result.context().availability().available()).isTrue();
        verify(operations).insertItem(eq(SCHEMA), eq("app_contract"), anyMap());
    }

    @Test
    void shouldExposeGeneratedRecordIdInCreateActionContext() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq("app_contract"), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        DynamicRecordService service = actionService(operations);
        DynamicRecord record = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001")
                .setValue("status", "draft");

        DynamicActionExecutionResult result = service.module(MODULE)
                .executeAction("create", DynamicActionExecutionRequest.record(record));

        assertThat(result.value()).isInstanceOf(String.class);
        assertThat(result.context().recordId()).isEqualTo(result.value());
    }

    @Test
    void shouldPublishActionExecutionEventAfterStandardAction() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq("app_contract"), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        CollectingRuntimeEventPublisher events = new CollectingRuntimeEventPublisher();
        DynamicRecordService service = actionService(operations, events);
        DynamicRecord record = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001")
                .setValue("status", "draft");
        record.setId("contract-1");

        service.module(MODULE).executeAction("create", DynamicActionExecutionRequest.record(record));

        assertThat(events.events()).extracting(RuntimeEvent::eventType)
                .containsExactly(RuntimeEventType.AFTER_CREATE, RuntimeEventType.ACTION_EXECUTED);
        assertThat(events.events().getFirst().mutationSource()).isEqualTo(RuntimeMutationSource.ACTION);
        RuntimeEvent action = events.events().getLast();
        assertThat(events.events().getFirst().traceId()).isEqualTo(action.traceId());
        assertThat(action.moduleAlias()).isEqualTo(MODULE);
        assertThat(action.entityAlias()).isEqualTo("contract");
        assertThat(action.recordId()).isEqualTo("contract-1");
        assertThat(action.actionCode()).isEqualTo("create");
        assertThat(action.payload()).containsEntry("executorType", "STANDARD")
                .containsEntry("result", "contract-1");
    }

    @Test
    void shouldBlockActionExecutionWhenAvailabilityFormulaFails() {
        DynamicRecordService service = actionService(operations());
        DynamicRecord submitted = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001")
                .setValue("status", "submitted");

        assertThatThrownBy(() -> service.module(MODULE)
                .executeAction("submit", DynamicActionExecutionRequest.record(submitted)))
                .isInstanceOf(DynamicActionExecutionException.class)
                .hasMessageContaining("只有草稿合同可以提交")
                .satisfies(error -> {
                    DynamicActionExecutionException exception = (DynamicActionExecutionException) error;
                    assertThat(exception.context().availability().available()).isFalse();
                    assertThat(exception.context().action().availabilityCondition()).isTrue();
                });
    }

    @Test
    void shouldRejectServiceActionExecutionWhenExecutorKeyIsMissing() {
        DynamicRecordService service = actionService(operations());
        DynamicRecord draft = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001")
                .setValue("status", "draft");

        assertThatThrownBy(() -> service.entity(MODULE, "contract")
                .executeAction("submit", DynamicActionExecutionRequest.record(draft)))
                .isInstanceOf(DynamicActionExecutionException.class)
                .hasMessageContaining("dynamic action executorKey must not be blank")
                .satisfies(error -> {
                    DynamicActionExecutionException exception = (DynamicActionExecutionException) error;
                    assertThat(exception.context().availability().available()).isTrue();
                    assertThat(exception.context().action().executorType())
                            .isEqualTo(net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType.SERVICE);
                });
    }

    @Test
    void shouldExecuteRegisteredServiceActionThroughStableActionApi() {
        RecordingActionExecutor executor = new RecordingActionExecutor();
        CollectingRuntimeEventPublisher events = new CollectingRuntimeEventPublisher();
        DynamicRecordService service = actionService(operations(), events, executor);
        DynamicRecord draft = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001")
                .setValue("status", "draft");
        draft.setId("contract-1");

        DynamicActionExecutionResult result = service.entity(MODULE, "contract")
                .executeAction("submit", DynamicActionExecutionRequest.record(draft));

        assertThat(result.value()).isEqualTo("submitted:contract-1");
        assertThat(executor.context().moduleAlias()).isEqualTo(MODULE);
        assertThat(executor.context().entityAlias()).isEqualTo("contract");
        assertThat(executor.context().actionCode()).isEqualTo("submit");
        assertThat(executor.context().availability().available()).isTrue();
        assertThat(executor.request().record()).isSameAs(draft);
        assertThat(events.events()).singleElement()
                .satisfies(event -> {
                    assertThat(event.eventType()).isEqualTo(RuntimeEventType.ACTION_EXECUTED);
                    assertThat(event.recordId()).isEqualTo("contract-1");
                    assertThat(event.actionCode()).isEqualTo("submit");
                    assertThat(event.traceId()).isEqualTo(result.context().traceId());
                    assertThat(event.payload()).containsEntry("executorType", "SERVICE")
                            .containsEntry("result", "submitted:contract-1");
                });
    }

    @Test
    void shouldBlockServiceActionWhenBeforeExecuteRuleFails() {
        RecordingActionExecutor executor = new RecordingActionExecutor();
        CollectingRuntimeEventPublisher events = new CollectingRuntimeEventPublisher();
        DynamicRecordService service = actionRuleService(operations(), events, executor);
        DynamicRecord draft = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001")
                .setValue("status", "draft")
                .setValue("amount", BigDecimal.ZERO);
        draft.setId("contract-1");

        assertThatThrownBy(() -> service.entity(MODULE, "contract")
                .executeAction("submit", DynamicActionExecutionRequest.record(draft)))
                .isInstanceOf(DynamicActionExecutionException.class)
                .hasCauseInstanceOf(DynamicFormulaException.class)
                .hasMessageContaining("提交金额必须大于0")
                .satisfies(error -> {
                    DynamicActionExecutionException exception = (DynamicActionExecutionException) error;
                    assertThat(exception.context().availability().available()).isTrue();
                    assertThat(exception.context().traceId()).isNotBlank();
                });
        assertThat(executor.context()).isNull();
        assertThat(events.events()).isEmpty();
    }

    @Test
    void shouldExecuteServiceActionWhenBeforeExecuteRulePasses() {
        RecordingActionExecutor executor = new RecordingActionExecutor();
        CollectingRuntimeEventPublisher events = new CollectingRuntimeEventPublisher();
        DynamicRecordService service = actionRuleService(operations(), events, executor);
        DynamicRecord draft = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001")
                .setValue("status", "draft")
                .setValue("amount", BigDecimal.ONE);
        draft.setId("contract-1");

        DynamicActionExecutionResult result = service.entity(MODULE, "contract")
                .executeAction("submit", DynamicActionExecutionRequest.record(draft));

        assertThat(result.value()).isEqualTo("submitted:contract-1");
        assertThat(events.events()).singleElement()
                .satisfies(event -> {
                    assertThat(event.eventType()).isEqualTo(RuntimeEventType.ACTION_EXECUTED);
                    assertThat(event.traceId()).isEqualTo(result.context().traceId());
                });
    }

    @Test
    void shouldLoadExistingRecordForBeforeExecuteRuleWhenRequestOnlyHasRecordId() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap()))
                .thenReturn(List.of(actionAmountRow("contract-1", "C-001", "draft", BigDecimal.ONE)));
        IdReturningActionExecutor executor = new IdReturningActionExecutor();
        DynamicRecordService service = actionRuleService(operations, RuntimeEventPublisher.noop(), executor);

        DynamicActionExecutionResult result = service.entity(MODULE, "contract")
                .executeAction("submit", DynamicActionExecutionRequest.id("contract-1"));

        assertThat(result.value()).isEqualTo("submitted:contract-1");
        assertThat(executor.context().recordId()).isEqualTo("contract-1");
        verify(operations, atLeastOnce()).query(anyString(), anyMap());
    }

    @Test
    void shouldKeepSubmittedChildrenForBeforeExecuteRuleWhenRequestHasNoMainFields() {
        IDatabaseOperations<Object> operations = operations();
        IdReturningActionExecutor executor = new IdReturningActionExecutor();
        DynamicRecordService service = childActionRuleService(operations, RuntimeEventPublisher.noop(), executor);
        DynamicRecord line = service.newRecord(MODULE, "line")
                .setValue("summary", "明细已填写");
        DynamicRecord draft = service.newRecord(MODULE, "contract")
                .setChildren("lines", List.of(line));
        draft.setId("contract-1");

        DynamicActionExecutionResult result = service.entity(MODULE, "contract")
                .executeAction("submit", DynamicActionExecutionRequest.record(draft));

        assertThat(result.value()).isEqualTo("submitted:contract-1");
        assertThat(executor.context().recordId()).isEqualTo("contract-1");
        verify(operations, never()).query(anyString(), anyMap());
    }

    @Test
    void shouldRejectServiceActionExecutionWhenExecutorIsNotRegistered() {
        DynamicRecordService service = actionService(operations(), RuntimeEventPublisher.noop(), null,
                submitActionWithExecutorKey("missingSubmitExecutor"));
        DynamicRecord draft = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001")
                .setValue("status", "draft");

        assertThatThrownBy(() -> service.entity(MODULE, "contract")
                .executeAction("submit", DynamicActionExecutionRequest.record(draft)))
                .isInstanceOf(DynamicActionExecutionException.class)
                .hasMessageContaining("unknown dynamic action executor key: missingSubmitExecutor");
    }

    @Test
    void shouldKeepActionContextWhenServiceActionExecutorFails() {
        DynamicRecordService service = actionService(operations(), RuntimeEventPublisher.noop(), new FailingActionExecutor());
        DynamicRecord draft = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001")
                .setValue("status", "draft");
        draft.setId("contract-1");

        assertThatThrownBy(() -> service.entity(MODULE, "contract")
                .executeAction("submit", DynamicActionExecutionRequest.record(draft)))
                .isInstanceOf(DynamicActionExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .satisfies(error -> {
                    DynamicActionExecutionException exception = (DynamicActionExecutionException) error;
                    assertThat(exception.context().actionCode()).isEqualTo("submit");
                    assertThat(exception.context().recordId()).isEqualTo("contract-1");
                    assertThat(exception.context().traceId()).isNotBlank();
                });
    }

    @Test
    void shouldRejectDuplicateDynamicActionExecutorKey() {
        assertThatThrownBy(() -> new DynamicActionExecutorRegistry(List.of(
                new TestActionExecutor("contractSubmit"),
                new TestActionExecutor("contractSubmit")
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate dynamic action executor key: contractSubmit");
    }

    @Test
    void shouldRegisterDynamicActionExecutorByMutatingRegistry() {
        DynamicActionExecutorRegistry registry = DynamicActionExecutorRegistry.empty();

        registry.register(new TestActionExecutor("contractSubmit"));

        assertThat(registry.contains("contractSubmit")).isTrue();
    }

    @Test
    void shouldRejectDynamicActionExecutorKeyWithSurroundingSpaces() {
        assertThatThrownBy(() -> new DynamicActionExecutorRegistry(List.of(new TestActionExecutor(" contractSubmit "))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not contain leading or trailing spaces");
    }

    @Test
    void shouldNotLoadExistingRecordForActionWithoutCondition() {
        IDatabaseOperations<Object> operations = operations();
        DynamicRecordService service = actionService(operations);
        DynamicRecord record = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001");
        record.setId("contract-1");

        DynamicActionAvailability availability = service.entity(MODULE, "contract")
                .actionAvailability("create", record);

        assertThat(availability.available()).isTrue();
        verify(operations, never()).query(anyString(), anyMap());
    }

    @Test
    void shouldMergeExistingValuesWhenEvaluatingActionAvailability() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(actionRow("contract-1", "C-001", "draft")));
        DynamicRecordService service = actionService(operations);
        DynamicRecord partial = service.newRecord(MODULE, "contract");
        partial.setId("contract-1");

        assertThat(service.entity(MODULE, "contract").actionAvailability("submit", partial).available()).isTrue();
        verify(operations).query(anyString(), anyMap());
    }

    @Test
    void shouldKeepNonMainEntityActionOutOfModuleActionAvailability() {
        ModuleDefinition module = new ModuleDefinition(
                MODULE,
                "Contract",
                List.of(actionEntity(), lineEntity()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        new EntityActionDefinition("contract", "submit", EntityActionKind.CUSTOM,
                                "提交", true, EntityActionStyle.PRIMARY)
                                .availableWhen("{status} == 'draft'"),
                        new EntityActionDefinition("line", "submit", EntityActionKind.CUSTOM,
                                "提交行", true, EntityActionStyle.NORMAL)
                                .availableWhen("{summary} != ''")
                ),
                "contract"
        );
        DynamicRecordService service = new DynamicRecordService(new DynamicRecordRuntime(operations()).register(module));
        DynamicRecord line = service.newRecord(MODULE, "line")
                .setValue("summary", "ok");

        assertThatThrownBy(() -> service.actionAvailability(MODULE, "submit", line))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dynamic record entity mismatch: line");
        assertThat(service.entity(MODULE, "line").actionAvailability("submit", line).available()).isTrue();
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
    void shouldExcludeSoftDeletedTargetWhenResolvingReference() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.row(anyString(), anyMap())).thenReturn(Map.of("total_count", 0));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of());
        DynamicRecordService service = referenceResolvingService(operations);

        DynamicReferenceResolveResponse response = service.resolveFieldReference(
                MODULE,
                "line",
                "contractId",
                DynamicReferenceResolveRequest.query("Deleted Contract")
        );

        assertThat(response.status()).isEqualTo(DynamicReferenceResolveStatus.NOT_FOUND);
        assertThat(response.options()).isEmpty();
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.atLeastOnce()).query(sql.capture(), anyMap());
        assertThat(sql.getAllValues()).anySatisfy(statement -> assertThat(statement)
                .contains("\"deleted\" =")
                .contains("\"deleted\" IS NULL"));
    }

    @Test
    void shouldExcludeSoftDeletedTargetWhenTranslatingReference() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.row(anyString(), anyMap())).thenReturn(Map.of("total_count", 0));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of());
        DynamicRecordService service = referenceResolvingService(operations);

        DynamicReferenceResolveResponse response = service.resolveFieldReference(
                MODULE,
                "line",
                "contractId",
                DynamicReferenceResolveRequest.translate(List.of("deleted-contract"))
        );

        assertThat(response.status()).isEqualTo(DynamicReferenceResolveStatus.NOT_FOUND);
        assertThat(response.results()).singleElement()
                .extracting(DynamicReferenceResolveResult::status)
                .isEqualTo(DynamicReferenceResolveStatus.NOT_FOUND);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(operations, org.mockito.Mockito.atLeastOnce()).query(sql.capture(), anyMap());
        assertThat(sql.getAllValues()).anySatisfy(statement -> assertThat(statement)
                .contains("\"deleted\" =")
                .contains("\"deleted\" IS NULL"));
    }

    @Test
    void shouldRejectSoftDeletedTargetWhenSavingReference() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap())).thenReturn(List.of());
        DynamicRecordService.EntityOperations lines = referenceResolvingService(operations).entity(MODULE, "line");
        DynamicRecord line = lines.newRecord()
                .setValue("contractId", "deleted-contract")
                .setValue("summary", "should fail");
        line.setId("line-1");

        assertThatThrownBy(() -> lines.create(line))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dynamic reference target not found")
                .hasMessageContaining("sales.contract.contract.deleted-contract");
        verify(operations, org.mockito.Mockito.never()).insertItem(anyString(), anyString(), anyMap());
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
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("unknown dynamic reference: sales.contract.line.contractId");
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
    void shouldExecuteEnableActionThroughStableActionApi() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(enabledRow("contract-1", false)));
        DynamicRecordService service = service(operations, enabledEntity());

        DynamicActionExecutionResult result = service.module(MODULE)
                .executeAction("enable", DynamicActionExecutionRequest.id("contract-1"));

        assertThat(result.value()).isEqualTo(1);
        assertThat(result.context().recordId()).isEqualTo("contract-1");
        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations).patchUpdateItemWhere(eq(SCHEMA), eq("app_contract"), body.capture(), anyMap());
        assertThat(body.getValue()).containsEntry("enabled", Boolean.TRUE);
    }

    @Test
    void shouldPublishRecordMutationEventsThroughStableServiceApi() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq("app_contract"), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(row("contract-1", "C-001", 0, false)));
        CollectingRuntimeEventPublisher events = new CollectingRuntimeEventPublisher();
        DynamicRecordService service = service(operations, contractEntity(), events);
        DynamicRecord record = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001");
        record.setId("contract-1");

        service.create(MODULE, "contract", record);
        service.update(MODULE, "contract", record);
        service.delete(MODULE, "contract", "contract-1");

        assertThat(events.events()).extracting(RuntimeEvent::eventType)
                .containsExactly(RuntimeEventType.AFTER_CREATE, RuntimeEventType.AFTER_UPDATE, RuntimeEventType.AFTER_DELETE);
        assertThat(events.events()).extracting(RuntimeEvent::mutationSource)
                .containsExactly(RuntimeMutationSource.BUSINESS, RuntimeMutationSource.BUSINESS, RuntimeMutationSource.BUSINESS);
        assertThat(events.events()).allSatisfy(event -> {
            assertThat(event.moduleAlias()).isEqualTo(MODULE);
            assertThat(event.entityAlias()).isEqualTo("contract");
            assertThat(event.recordId()).isEqualTo("contract-1");
            assertThat(event.actionCode()).isNull();
        });
    }

    @Test
    void shouldPublishBatchDeleteAndSortMutationEvents() {
        IDatabaseOperations<Object> operations = operations();
        stubSortableRows(operations);
        CollectingRuntimeEventPublisher events = new CollectingRuntimeEventPublisher();
        DynamicRecordService service = service(operations, sortableEntity(), events);

        service.reorder(MODULE, "contract", List.of("first", "second", "third"));
        service.moveBefore(MODULE, "contract", "third", "first");
        service.deleteBatch(MODULE, "contract", List.of("first", "second"));

        assertThat(events.events()).extracting(RuntimeEvent::eventType)
                .containsExactly(RuntimeEventType.AFTER_UPDATE, RuntimeEventType.AFTER_UPDATE, RuntimeEventType.AFTER_DELETE);
        assertThat(events.events().get(0).payload())
                .containsEntry("operation", "reorder")
                .containsEntry("recordIds", List.of("first", "second", "third"));
        assertThat(events.events().get(1).payload())
                .containsEntry("operation", "moveBefore")
                .containsEntry("beforeId", "first");
        assertThat(events.events().get(2).payload())
                .containsEntry("recordIds", List.of("first", "second"))
                .containsEntry("count", 2);
    }

    @Test
    void shouldPublishRuntimeEventAfterCommitWhenTransactionIsActive() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq("app_contract"), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        CollectingRuntimeEventPublisher events = new CollectingRuntimeEventPublisher();
        DynamicRecordService service = service(operations, contractEntity(), events);
        DynamicRecord record = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001");
        record.setId("contract-1");

        try {
            TransactionSynchronizationManager.initSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(true);
            service.create(MODULE, "contract", record);
            assertThat(events.events()).isEmpty();
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            assertThat(events.events()).extracting(RuntimeEvent::eventType)
                    .containsExactly(RuntimeEventType.AFTER_CREATE);
        } finally {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization();
            }
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void shouldNotPublishRuntimeEventAfterRollback() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq("app_contract"), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        CollectingRuntimeEventPublisher events = new CollectingRuntimeEventPublisher();
        DynamicRecordService service = service(operations, contractEntity(), events);
        DynamicRecord record = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001");
        record.setId("contract-1");

        try {
            TransactionSynchronizationManager.initSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(true);
            service.create(MODULE, "contract", record);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            assertThat(events.events()).isEmpty();
        } finally {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization();
            }
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void shouldRejectIncompleteRuntimeEvent() {
        assertThatThrownBy(() -> new RuntimeEvent(null, null, null, MODULE, "contract", null, null,
                null, false, RuntimeMutationSource.BUSINESS, Map.of(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventType");
        assertThatThrownBy(() -> RuntimeEvent.of(RuntimeEventType.ACTION_EXECUTED, MODULE, "contract", "contract-1",
                null, null, false, RuntimeMutationSource.ACTION, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actionCode");
    }

    @Test
    void shouldExecuteReferenceOptionsActionWithExplicitPageRequest() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.row(anyString(), anyMap())).thenReturn(Map.of("total_count", 1));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(referenceRow("contract-1", "Contract One")));
        DynamicRecordService service = service(operations, referenceEntity());

        DynamicActionExecutionResult result = service.entity(MODULE, "contract")
                .executeAction("referenceOptions", DynamicActionExecutionRequest.empty()
                        .withCriteria(Criteria.of().eq("code", "CONTRACT-1"))
                        .withPageRequest(PageRequest.of(1, 10)));

        assertThat(result.value()).isInstanceOf(net.ximatai.muyun.database.core.orm.PageResult.class);
        @SuppressWarnings("unchecked")
        net.ximatai.muyun.database.core.orm.PageResult<ReferenceOption> options =
                (net.ximatai.muyun.database.core.orm.PageResult<ReferenceOption>) result.value();
        assertThat(options.getRecords()).containsExactly(new ReferenceOption("contract-1", "Contract One"));
    }

    @Test
    void shouldRequirePageRequestForPagedActionExecution() {
        DynamicRecordService service = service(operations(), referenceEntity());

        assertThatThrownBy(() -> service.entity(MODULE, "contract")
                .executeAction("referenceOptions", DynamicActionExecutionRequest.empty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pageRequest");
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
    void shouldRejectSaveWhenDynamicFormulaValidationFailsThroughStableServiceApi() {
        IDatabaseOperations<Object> operations = operations();
        DynamicRecordService service = service(operations, formulaValidationEntity());
        DynamicRecord record = service.newRecord(MODULE, "contract")
                .setValue("amount", BigDecimal.valueOf(-1));
        record.setId("contract-1");

        assertThatThrownBy(() -> service.create(MODULE, "contract", record))
                .isInstanceOf(DynamicFormulaException.class)
                .hasMessageContaining("amount must be positive")
                .satisfies(error -> {
                    DynamicFormulaException exception = (DynamicFormulaException) error;
                    assertThat(exception.firstError().ruleId()).isEqualTo("amountPositive");
                    assertThat(exception.firstError().fieldPath()).isEqualTo("amount");
                });
        verify(operations, never()).insertItem(anyString(), anyString(), anyMap());
    }

    @Test
    void shouldKeepFormulaWarningsOnRecordThroughStableServiceApi() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq("app_contract"), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        DynamicRecordService service = service(operations, formulaWarningEntity());
        DynamicRecord record = service.newRecord(MODULE, "contract")
                .setValue("amount", BigDecimal.valueOf(1500));
        record.setId("contract-1");

        assertThat(service.create(MODULE, "contract", record)).isEqualTo("contract-1");

        assertThat(record.formulaReport().warnings())
                .hasSize(1)
                .first()
                .satisfies(issue -> {
                    assertThat(issue.ruleId()).isEqualTo("amountHighRisk");
                    assertThat(issue.message()).isEqualTo("amount is high");
                });
        verify(operations).insertItem(eq(SCHEMA), eq("app_contract"), anyMap());
    }

    @Test
    void shouldApplyFieldDefaultAndRejectWriteProtectedInput() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.insertItem(eq(SCHEMA), eq("app_contract"), anyMap()))
                .thenAnswer(invocation -> invocation.<Map<String, Object>>getArgument(2).get("id"));
        DynamicRecordService service = service(operations, behaviorEntity());
        DynamicRecord record = service.newRecord(MODULE, "contract")
                .setValue("code", "C-001");
        record.setId("contract-1");

        assertThat(service.create(MODULE, "contract", record)).isEqualTo("contract-1");
        assertThatThrownBy(() -> {
            DynamicRecord invalid = service.newRecord(MODULE, "contract")
                    .setValue("code", "C-002")
                    .setValue("serverCode", "MANUAL");
            invalid.setId("contract-2");
            service.create(MODULE, "contract", invalid);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("write protected");

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations).insertItem(eq(SCHEMA), eq("app_contract"), body.capture());
        assertThat(body.getValue()).containsEntry("status", "draft");
        assertThat(body.getValue()).doesNotContainKey("server_code");
    }

    @Test
    void shouldRejectWriteProtectedDynamicFieldOnUpdate() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.row(anyString(), anyMap())).thenReturn(Map.of("total_count", 1));
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(behaviorRow("contract-1", "C-001", "SYS-1")));
        DynamicRecordService service = service(operations, behaviorEntity());
        DynamicRecord record = service.newRecord(MODULE, "contract")
                .setValue("serverCode", "MANUAL");
        record.setId("contract-1");

        assertThatThrownBy(() -> service.update(MODULE, "contract", record))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("write protected");
    }

    @Test
    void shouldAllowPlatformAbilityWriteOnWriteProtectedField() {
        IDatabaseOperations<Object> operations = operations();
        when(operations.query(anyString(), anyMap())).thenReturn(List.of(enabledRow("contract-1", false)));
        when(operations.patchUpdateItemWhere(anyString(), anyString(), anyMap(), anyMap())).thenReturn(1);
        DynamicRecordService service = service(operations, writeProtectedEnabledEntity());

        assertThat(service.enable(MODULE, "contract", "contract-1")).isEqualTo(1);

        ArgumentCaptor<Map<String, Object>> body = mapCaptor();
        verify(operations).patchUpdateItemWhere(eq(SCHEMA), eq("app_contract"), body.capture(), anyMap());
        assertThat(body.getValue()).containsEntry("enabled", Boolean.TRUE);
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
        return service(operations, entity, RuntimeEventPublisher.noop());
    }

    private DynamicRecordService service(IDatabaseOperations<Object> operations,
                                         EntityDefinition entity,
                                         RuntimeEventPublisher eventPublisher) {
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(
                operations,
                new DynamicModuleRegistry(),
                DynamicFieldValueValidator.NONE,
                eventPublisher
        )
                .register(new ModuleDefinition(MODULE, "Contract", List.of(entity)));
        return new DynamicRecordService(runtime);
    }

    private DynamicRecordService actionService(IDatabaseOperations<Object> operations) {
        return actionService(operations, RuntimeEventPublisher.noop());
    }

    private DynamicRecordService actionService(IDatabaseOperations<Object> operations, RuntimeEventPublisher eventPublisher) {
        return actionService(operations, eventPublisher, null,
                new EntityActionDefinition("contract", "submit", EntityActionKind.CUSTOM,
                        "提交", true, EntityActionStyle.PRIMARY)
                        .availableWhen("{status} == 'draft'", "只有草稿合同可以提交"));
    }

    private DynamicRecordService actionService(IDatabaseOperations<Object> operations,
                                               RuntimeEventPublisher eventPublisher,
                                               DynamicActionExecutor executor) {
        return actionService(operations, eventPublisher, executor, submitActionWithExecutorKey("contractSubmit"));
    }

    private DynamicRecordService actionService(IDatabaseOperations<Object> operations,
                                               RuntimeEventPublisher eventPublisher,
                                               DynamicActionExecutor executor,
                                               EntityActionDefinition submitAction) {
        ModuleDefinition module = new ModuleDefinition(
                MODULE,
                "Contract",
                List.of(actionEntity()),
                List.of(),
                List.of(),
                List.of(),
                List.of(submitAction)
        );
        DynamicActionExecutorRegistry executorRegistry = executor == null
                ? DynamicActionExecutorRegistry.empty()
                : new DynamicActionExecutorRegistry(List.of(executor));
        return new DynamicRecordService(new DynamicRecordRuntime(
                operations,
                new DynamicModuleRegistry(),
                DynamicFieldValueValidator.NONE,
                eventPublisher,
                executorRegistry
        ).register(module));
    }

    private DynamicRecordService actionRuleService(IDatabaseOperations<Object> operations,
                                                   RuntimeEventPublisher eventPublisher,
                                                   DynamicActionExecutor executor) {
        ModuleDefinition module = new ModuleDefinition(
                MODULE,
                "Contract",
                List.of(actionRuleEntity()),
                List.of(),
                List.of(),
                List.of(),
                List.of(submitActionWithExecutorKey("contractSubmit"))
        );
        DynamicActionExecutorRegistry executorRegistry = executor == null
                ? DynamicActionExecutorRegistry.empty()
                : new DynamicActionExecutorRegistry(List.of(executor));
        return new DynamicRecordService(new DynamicRecordRuntime(
                operations,
                new DynamicModuleRegistry(),
                DynamicFieldValueValidator.NONE,
                eventPublisher,
                executorRegistry
        ).register(module));
    }

    private DynamicRecordService childActionRuleService(IDatabaseOperations<Object> operations,
                                                        RuntimeEventPublisher eventPublisher,
                                                        DynamicActionExecutor executor) {
        ModuleDefinition module = new ModuleDefinition(
                MODULE,
                "Contract",
                List.of(actionChildRuleEntity(), lineEntity()),
                List.of(EntityRelationDefinition.child("lines", "contract", "line", "contractId")),
                List.of(),
                List.of(),
                List.of(submitActionWithoutAvailability("contractSubmit"))
        );
        DynamicActionExecutorRegistry executorRegistry = executor == null
                ? DynamicActionExecutorRegistry.empty()
                : new DynamicActionExecutorRegistry(List.of(executor));
        return new DynamicRecordService(new DynamicRecordRuntime(
                operations,
                new DynamicModuleRegistry(),
                DynamicFieldValueValidator.NONE,
                eventPublisher,
                executorRegistry
        ).register(module));
    }

    private EntityActionDefinition submitActionWithExecutorKey(String executorKey) {
        return new EntityActionDefinition("contract", "submit", EntityActionKind.CUSTOM,
                "提交", true, EntityActionLevel.RECORD, EntityActionStyle.PRIMARY,
                EntityActionCategory.CUSTOM, null, null, null, null,
                "{status} == 'draft'", "只有草稿合同可以提交",
                EntityActionExecutorType.SERVICE, executorKey
        );
    }

    private EntityActionDefinition submitActionWithoutAvailability(String executorKey) {
        return new EntityActionDefinition("contract", "submit", EntityActionKind.CUSTOM,
                "提交", true, EntityActionLevel.RECORD, EntityActionStyle.PRIMARY,
                EntityActionCategory.CUSTOM, null, null, null, null,
                null, null, EntityActionExecutorType.SERVICE, executorKey
        );
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

    private EntityDefinition actionEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required(),
                        FieldDefinition.string("status", "Status").length(32)
                )
        );
    }

    private EntityDefinition actionRuleEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required(),
                        FieldDefinition.string("status", "Status").length(32),
                        FieldDefinition.decimal("amount", "Amount").precision(18, 2)
                )
        ).withFormulaRules(EntityFormulaRuleDefinition
                .validation("submitAmountPositive", "amount", "{amount} > 0", "提交金额必须大于0")
                .phase(FormulaRulePhase.ACTION_BEFORE_EXECUTE));
    }

    private EntityDefinition actionChildRuleEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64),
                        FieldDefinition.string("status", "Status").length(32)
                )
        ).withFormulaRules(EntityFormulaRuleDefinition
                .validation("submitLineRequired", "lines.summary", "COUNT({lines.summary}) > 0", "提交前必须填写明细")
                .phase(FormulaRulePhase.ACTION_BEFORE_EXECUTE));
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

    private EntityDefinition behaviorEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required().validationRegex("C-[0-9]+"),
                        FieldDefinition.string("status", "Status").defaultValue("draft"),
                        FieldDefinition.string("serverCode", "Server Code").column("server_code").writeProtected()
                )
        );
    }

    private EntityDefinition writeProtectedEnabledEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required(),
                        FieldDefinition.enabled().writeProtected()
                )
        ).withCapabilities(EntityCapability.CRUD, EntityCapability.ENABLE);
    }

    private EntityDefinition formulaValidationEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(FieldDefinition.decimal("amount", "Amount").precision(18, 2))
        ).withFormulaRules(EntityFormulaRuleDefinition
                .validation("amountPositive", "amount", "{amount} > 0", "amount must be positive"));
    }

    private EntityDefinition formulaWarningEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(FieldDefinition.decimal("amount", "Amount").precision(18, 2))
        ).withFormulaRules(EntityFormulaRuleDefinition
                .validation("amountHighRisk", "amount", "{amount} < 1000", "amount is high")
                .severity(FormulaIssueLevel.WARNING));
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

    private Map<String, Object> actionRow(String id, String code, String status) {
        return Map.of(
                "id", id,
                "code", code,
                "status", status,
                "deleted", Boolean.FALSE,
                "version", 0
        );
    }

    private Map<String, Object> actionAmountRow(String id, String code, String status, BigDecimal amount) {
        return Map.of(
                "id", id,
                "code", code,
                "status", status,
                "amount", amount,
                "deleted", Boolean.FALSE,
                "version", 0
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

    private Map<String, Object> behaviorRow(String id, String code, String serverCode) {
        return Map.of(
                "id", id,
                "code", code,
                "status", "draft",
                "server_code", serverCode,
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

    private static final class CollectingRuntimeEventPublisher implements RuntimeEventPublisher {
        private final List<RuntimeEvent> events = new ArrayList<>();

        @Override
        public void publish(RuntimeEvent event) {
            events.add(event);
        }

        List<RuntimeEvent> events() {
            return events;
        }
    }

    private static final class RecordingActionExecutor implements DynamicActionExecutor {
        private DynamicActionExecutionContext context;
        private DynamicActionExecutionRequest request;

        @Override
        public String executorKey() {
            return "contractSubmit";
        }

        @Override
        public Object execute(DynamicActionExecutionContext context, DynamicActionExecutionRequest request) {
            this.context = context;
            this.request = request;
            return "submitted:" + request.record().getId();
        }

        DynamicActionExecutionContext context() {
            return context;
        }

        DynamicActionExecutionRequest request() {
            return request;
        }
    }

    private record TestActionExecutor(String executorKey) implements DynamicActionExecutor {
        @Override
        public Object execute(DynamicActionExecutionContext context, DynamicActionExecutionRequest request) {
            return null;
        }
    }

    private static final class FailingActionExecutor implements DynamicActionExecutor {
        @Override
        public String executorKey() {
            return "contractSubmit";
        }

        @Override
        public Object execute(DynamicActionExecutionContext context, DynamicActionExecutionRequest request) {
            throw new IllegalStateException("submit failed");
        }
    }

    private static final class IdReturningActionExecutor implements DynamicActionExecutor {
        private DynamicActionExecutionContext context;

        @Override
        public String executorKey() {
            return "contractSubmit";
        }

        @Override
        public Object execute(DynamicActionExecutionContext context, DynamicActionExecutionRequest request) {
            this.context = context;
            return "submitted:" + context.recordId();
        }

        DynamicActionExecutionContext context() {
            return context;
        }
    }
}
