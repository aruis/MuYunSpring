package net.ximatai.muyun.spring.platform.audit;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.event.RuntimeEventType;
import net.ximatai.muyun.spring.common.formula.FormulaRulePhase;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionKind;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionStyle;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityFormulaRuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionResult;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutorRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionResultBody;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionResultType;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicModuleRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicFieldValueValidator;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuntimeActionExecutionClosureTest {
    private static final String MODULE = "sales.contract";
    private final RuntimeAuditRecordService auditService = new RuntimeAuditRecordService(new TestMemoryDao<>());
    private final DynamicRecordService recordService = recordService(new RuntimeAuditEventListener(auditService));

    @Test
    void shouldAuditSuccessfulContractSubmitActionAsCompleteRuntimeClosure() {
        DynamicRecord draft = contract("contract-1", "draft", BigDecimal.TEN);

        DynamicActionExecutionResult result = recordService.entity(MODULE, "contract")
                .executeAction("submit", DynamicActionExecutionRequest.record(draft)
                        .withPayloadValue("comment", "同意提交"));

        assertThat(result.body().type()).isEqualTo(DynamicActionResultType.VALUE);
        assertThat(result.body().message()).isEqualTo("已提交");
        assertThat(result.body().refresh()).isTrue();
        assertThat(result.context().recordId()).isEqualTo("contract-1");

        List<RuntimeAuditRecord> trace = auditService.traceEvents(result.context().traceId(), PageRequest.of(1, 10));
        assertThat(trace).singleElement().satisfies(record -> {
            assertThat(record.getEventType()).isEqualTo(RuntimeEventType.ACTION_EXECUTED);
            assertThat(record.getModuleAlias()).isEqualTo(MODULE);
            assertThat(record.getEntityAlias()).isEqualTo("contract");
            assertThat(record.getRecordId()).isEqualTo("contract-1");
            assertThat(record.getActionCode()).isEqualTo("submit");
            assertThat(record.getExecutorType()).isEqualTo("SERVICE");
            assertThat(record.getResultType()).isEqualTo("VALUE");
            assertThat(record.getResultMessage()).isEqualTo("已提交");
            assertThat(record.getRefreshRequested()).isTrue();
            assertThat(record.getResultText()).isEqualTo("submitted:同意提交");
        });
    }

    @Test
    void shouldAuditContractSubmitAvailabilityFailureAsActionFailure() {
        DynamicRecord submitted = contract("contract-1", "submitted", BigDecimal.TEN);

        DynamicActionExecutionException exception = actionFailure(() -> recordService.entity(MODULE, "contract")
                .executeAction("submit", DynamicActionExecutionRequest.record(submitted)));

        assertThat(exception).hasMessageContaining("只有草稿合同可以提交");
        assertThat(exception.failureStage()).isEqualTo(DynamicActionExecutionException.STAGE_AVAILABILITY);
        assertActionFailureAuditTrace(exception, "availability");

        assertThat(auditService.failedActions(MODULE, PageRequest.of(1, 10)))
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.getActionCode()).isEqualTo("submit");
                    assertThat(record.getRecordId()).isEqualTo("contract-1");
                    assertThat(record.getExecutorType()).isEqualTo("SERVICE");
                    assertThat(record.getFailureStage()).isEqualTo("availability");
                    assertThat(record.getErrorMessage()).isEqualTo("只有草稿合同可以提交");
                });
    }

    @Test
    void shouldAuditContractSubmitBeforeRuleFailureAsActionFailure() {
        DynamicRecord draft = contract("contract-1", "draft", BigDecimal.ZERO);

        DynamicActionExecutionException exception = actionFailure(() -> recordService.entity(MODULE, "contract")
                .executeAction("submit", DynamicActionExecutionRequest.record(draft)));

        assertThat(exception).hasMessageContaining("提交金额必须大于0");
        assertThat(exception.failureStage()).isEqualTo(DynamicActionExecutionException.STAGE_BEFORE_EXECUTE_RULE);
        assertActionFailureAuditTrace(exception, "beforeExecuteRule");

        assertThat(auditService.failedActions(MODULE, PageRequest.of(1, 10)))
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.getActionCode()).isEqualTo("submit");
                    assertThat(record.getRecordId()).isEqualTo("contract-1");
                    assertThat(record.getFailureStage()).isEqualTo("beforeExecuteRule");
                    assertThat(record.getErrorType()).contains("DynamicFormulaException");
                    assertThat(record.getErrorMessage()).contains("提交金额必须大于0");
                });
    }

    private DynamicActionExecutionException actionFailure(Runnable action) {
        Throwable thrown = catchThrowable(action::run);
        assertThat(thrown).isInstanceOf(DynamicActionExecutionException.class);
        return (DynamicActionExecutionException) thrown;
    }

    private void assertActionFailureAuditTrace(DynamicActionExecutionException exception, String failureStage) {
        assertThat(auditService.traceEvents(exception.context().traceId(), PageRequest.of(1, 10)))
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.getEventType()).isEqualTo(RuntimeEventType.ACTION_FAILED);
                    assertThat(record.getTraceId()).isEqualTo(exception.context().traceId());
                    assertThat(record.getModuleAlias()).isEqualTo(MODULE);
                    assertThat(record.getEntityAlias()).isEqualTo("contract");
                    assertThat(record.getRecordId()).isEqualTo("contract-1");
                    assertThat(record.getActionCode()).isEqualTo("submit");
                    assertThat(record.getFailureStage()).isEqualTo(failureStage);
                });
    }

    private DynamicRecordService recordService(RuntimeAuditEventListener listener) {
        DynamicActionExecutorRegistry executors = new DynamicActionExecutorRegistry(List.of(new SubmitExecutor()));
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(
                operations(),
                new DynamicModuleRegistry(),
                DynamicFieldValueValidator.NONE,
                listener::onRuntimeEvent,
                executors
        ).register(module());
        return new DynamicRecordService(runtime);
    }

    private ModuleDefinition module() {
        return new ModuleDefinition(
                MODULE,
                "Contract",
                List.of(contractEntity()),
                List.of(),
                List.of(),
                List.of(),
                List.of(submitAction())
        );
    }

    private EntityDefinition contractEntity() {
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

    private EntityActionDefinition submitAction() {
        return new EntityActionDefinition("contract", "submit", EntityActionKind.CUSTOM,
                "提交", true, EntityActionLevel.RECORD, EntityActionStyle.PRIMARY,
                EntityActionCategory.CUSTOM, null, null, null, null,
                "{status} == 'draft'", "只有草稿合同可以提交",
                EntityActionExecutorType.SERVICE, "contractSubmit"
        );
    }

    private DynamicRecord contract(String id, String status, BigDecimal amount) {
        DynamicRecord record = recordService.newRecord(MODULE, "contract")
                .setValue("code", "C-001")
                .setValue("status", status)
                .setValue("amount", amount);
        record.setId(id);
        return record;
    }

    private IDatabaseOperations<Object> operations() {
        @SuppressWarnings("unchecked")
        IDatabaseOperations<Object> operations = mock(IDatabaseOperations.class);
        when(operations.getDBInfo()).thenReturn(new DBInfo("POSTGRESQL").setName("muyun_test"));
        return operations;
    }

    private static final class SubmitExecutor implements DynamicActionExecutor {
        @Override
        public String executorKey() {
            return "contractSubmit";
        }

        @Override
        public Object execute(net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionContext context,
                              DynamicActionExecutionRequest request) {
            return DynamicActionResultBody.refreshed("submitted:" + request.payload().get("comment"))
                    .message("已提交");
        }
    }
}
