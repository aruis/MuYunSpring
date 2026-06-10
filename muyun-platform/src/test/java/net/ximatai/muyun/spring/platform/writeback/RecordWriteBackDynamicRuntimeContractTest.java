package net.ximatai.muyun.spring.platform.writeback;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.AllowAllActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.AllowAllDataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicFieldValueValidator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicModuleRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationCoordinator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationCoordinators;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationEvent;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.impact.RecordImpactOriginCoordinator;
import net.ximatai.muyun.spring.platform.impact.RecordImpactRelationService;
import net.ximatai.muyun.spring.platform.impact.RecordImpactType;
import net.ximatai.muyun.spring.platform.impact.RecordOriginContext;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecordWriteBackDynamicRuntimeContractTest {
    @Test
    void shouldWriteBackTargetRecordThroughDynamicMutationRuntime() {
        RuntimeFixture fixture = runtimeFixture();
        fixture.saveRule(baseRule());
        fixture.createInvoice("C-001", BigDecimal.ZERO);

        DynamicRecord contract = fixture.service.newRecord("sales.contract", "contract")
                .setValue("contractNo", "C-001")
                .setValue("amount", BigDecimal.TEN);
        fixture.service.create("sales.contract", "contract", contract);

        List<Map<String, Object>> invoiceRows = fixture.table("app_invoice");
        assertThat(invoiceRows).singleElement()
                .satisfies(row -> assertThat(row.get("received_amount")).isEqualTo(BigDecimal.TEN));
        String invoiceId = String.valueOf(invoiceRows.getFirst().get("id"));
        assertThat(fixture.mutationEvents)
                .extracting(DynamicRecordMutationEvent::moduleAlias)
                .containsExactly("finance.invoice", "sales.contract", "finance.invoice");
        assertThat(fixture.mutationEvents.get(2).shouldSkipForSingleHopCascade()).isTrue();
        assertThat(fixture.effectLogService.selectByTarget("finance.invoice", invoiceId, null))
                .singleElement()
                .satisfies(effect -> {
                    assertThat(effect.getTargetField()).isEqualTo("receivedAmount");
                    assertThat(effect.getBeforeValue()).isEqualTo("0");
                    assertThat(effect.getAfterValue()).isEqualTo("10");
                });
    }

    @Test
    void shouldFailSourceSaveWhenWriteBackTargetCannotBeResolved() {
        RuntimeFixture fixture = runtimeFixture();
        fixture.saveRule(baseRule());
        DynamicRecord contract = fixture.service.newRecord("sales.contract", "contract")
                .setValue("contractNo", "C-MISSING")
                .setValue("amount", BigDecimal.TEN);

        assertThatThrownBy(() -> fixture.service.create("sales.contract", "contract", contract))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("target record not found");
        assertThat(fixture.effectLogService.selectByTraceId(fixture.mutationEvents.get(0).traceId(), null)).isEmpty();
    }

    @Test
    void shouldWriteBackGeneratedRecordToSourceRecordByImpactRelation() {
        RuntimeFixture fixture = runtimeFixture();
        DynamicRecord contract = fixture.service.newRecord("sales.contract", "contract")
                .setValue("contractNo", "C-REL")
                .setValue("amount", BigDecimal.ZERO);
        fixture.service.create("sales.contract", "contract", contract);
        String contractId = String.valueOf(fixture.table("app_contract").getFirst().get("id"));
        fixture.impactRelationService.registerFromOriginContext(new RecordOriginContext(
                RecordImpactType.GENERATE_PUSH,
                "sales.contract",
                contractId,
                "finance.invoice",
                "gen-1",
                "pushInvoice",
                "batch-1",
                "draft-1"
        ), "invoice-gen-1", "user-1");
        fixture.saveRule(relationRule());

        DynamicRecord invoice = fixture.service.newRecord("finance.invoice", "invoice")
                .setValue("contractNo", "C-REL")
                .setValue("receivedAmount", BigDecimal.TEN);
        invoice.setId("invoice-gen-1");
        fixture.service.create("finance.invoice", "invoice", invoice);

        assertThat(fixture.table("app_contract")).singleElement()
                .satisfies(row -> assertThat(row.get("amount")).isEqualTo(BigDecimal.TEN));
        assertThat(fixture.effectLogService.selectByTarget("sales.contract", contractId, null))
                .singleElement()
                .satisfies(effect -> {
                    assertThat(effect.getTriggerModuleAlias()).isEqualTo("finance.invoice");
                    assertThat(effect.getTriggerRecordId()).isEqualTo("invoice-gen-1");
                    assertThat(effect.getTargetField()).isEqualTo("amount");
                    assertThat(effect.getBeforeValue()).isEqualTo("0");
                    assertThat(effect.getAfterValue()).isEqualTo("10");
                });
    }

    @Test
    void shouldRegisterSplitGeneratedTargetRootsAndWriteBackByImpactRelation() {
        RuntimeFixture fixture = runtimeFixture();
        DynamicRecord contract = fixture.service.newRecord("sales.contract", "contract")
                .setValue("contractNo", "C-SPLIT")
                .setValue("amount", BigDecimal.ZERO);
        fixture.service.create("sales.contract", "contract", contract);
        String contractId = String.valueOf(fixture.table("app_contract").getFirst().get("id"));
        fixture.saveRule(relationRule());

        DynamicRecord firstInvoice = fixture.service.newRecord("finance.invoice", "invoice")
                .setValue("contractNo", "C-SPLIT")
                .setValue("receivedAmount", new BigDecimal("20.00"))
                .putMutationMetadata(RecordImpactOriginCoordinator.ORIGIN_CONTEXT_KEY,
                        originContext(contractId, "batch-split-1", "invoice:1"));
        String firstInvoiceId = fixture.service.create("finance.invoice", "invoice", firstInvoice);
        DynamicRecord secondInvoice = fixture.service.newRecord("finance.invoice", "invoice")
                .setValue("contractNo", "C-SPLIT")
                .setValue("receivedAmount", new BigDecimal("30.00"))
                .putMutationMetadata(RecordImpactOriginCoordinator.ORIGIN_CONTEXT_KEY,
                        originContext(contractId, "batch-split-1", "invoice:2"));
        String secondInvoiceId = fixture.service.create("finance.invoice", "invoice", secondInvoice);

        assertThat(fixture.impactRelationService.listBySource("sales.contract", contractId))
                .extracting(relation -> relation.getTargetRecordId() + ":" + relation.getDraftKey())
                .containsExactlyInAnyOrder(firstInvoiceId + ":invoice:1", secondInvoiceId + ":invoice:2");
        assertThat(fixture.table("app_contract")).singleElement()
                .satisfies(row -> assertThat(row.get("amount")).isEqualTo(new BigDecimal("30.00")));
        assertThat(fixture.effectLogService.selectByTarget("sales.contract", contractId, null))
                .extracting(RecordWriteBackEffectLog::getTriggerRecordId)
                .containsExactlyInAnyOrder(firstInvoiceId, secondInvoiceId);
    }

    @Test
    void shouldWriteBackToMatchedTargetChildRowThroughDynamicRuntime() {
        RuntimeFixture fixture = runtimeFixture();
        fixture.saveRule(childLineRule());
        DynamicRecord invoice = fixture.service.newRecord("finance.invoice", "invoice")
                .setValue("contractNo", "C-LINE")
                .setValue("receivedAmount", BigDecimal.ZERO);
        DynamicRecord line = fixture.service.newRecord("finance.invoice", "invoice_line")
                .setValue("lineNo", "C-LINE")
                .setValue("receivedAmount", BigDecimal.ZERO);
        List<DynamicRecord> lines = new ArrayList<>();
        lines.add(line);
        for (int i = 0; i < 501; i++) {
            lines.add(fixture.service.newRecord("finance.invoice", "invoice_line")
                    .setValue("lineNo", "OTHER-" + i)
                    .setValue("receivedAmount", BigDecimal.ZERO));
        }
        invoice.setChildren("lines", lines);
        fixture.service.create("finance.invoice", "invoice", invoice);

        DynamicRecord contract = fixture.service.newRecord("sales.contract", "contract")
                .setValue("contractNo", "C-LINE")
                .setValue("amount", new BigDecimal("12.00"));
        fixture.service.create("sales.contract", "contract", contract);

        assertThat(fixture.table("app_invoice")).singleElement()
                .satisfies(row -> assertThat(row.get("received_amount")).isEqualTo(BigDecimal.ZERO));
        assertThat(fixture.table("app_invoice_line")).hasSize(502);
        assertThat(fixture.table("app_invoice_line").stream()
                .filter(row -> "C-LINE".equals(row.get("line_no")))
                .toList()).singleElement()
                .satisfies(row -> assertThat(row.get("received_amount")).isEqualTo(new BigDecimal("12.00")));
        assertThat(fixture.table("app_invoice_line").stream()
                .filter(row -> "OTHER-500".equals(row.get("line_no")))
                .toList()).singleElement()
                .satisfies(row -> assertThat(row.get("received_amount")).isEqualTo(BigDecimal.ZERO));
        String lineId = String.valueOf(fixture.table("app_invoice_line").stream()
                .filter(row -> "C-LINE".equals(row.get("line_no")))
                .findFirst()
                .orElseThrow()
                .get("id"));
        assertThat(fixture.effectLogService.selectByTarget("finance.invoice", lineId, null))
                .singleElement()
                .satisfies(effect -> {
                    assertThat(effect.getTargetField()).isEqualTo("receivedAmount");
                    assertThat(effect.getBeforeValue()).isEqualTo("0");
                    assertThat(effect.getAfterValue()).isEqualTo("12.00");
                });
        assertThat(fixture.childLifecycleEvents())
                .containsExactly("before:lines:invoice_line:" + lineId, "after:lines:invoice_line:" + lineId);
    }

    @Test
    void shouldLocateWriteBackTargetBySystemQueryWhenDataScopeHidesTarget() {
        RuntimeFixture fixture = runtimeFixture(new HideFinanceInvoiceDataScopeCriteriaService());
        fixture.saveRule(baseRule());
        fixture.createInvoice("C-SCOPE", BigDecimal.ZERO);

        assertThat(fixture.service.list("finance.invoice", "invoice", Criteria.of(), net.ximatai.muyun.database.core.orm.PageRequest.of(1, 10)))
                .isEmpty();

        DynamicRecord contract = fixture.service.newRecord("sales.contract", "contract")
                .setValue("contractNo", "C-SCOPE")
                .setValue("amount", new BigDecimal("18.00"));
        fixture.service.create("sales.contract", "contract", contract);

        assertThat(fixture.table("app_invoice")).singleElement()
                .satisfies(row -> assertThat(row.get("received_amount")).isEqualTo(new BigDecimal("18.00")));
    }

    @Test
    void shouldApplyAddWriteBackWhenDynamicRecordEntersEffectiveState() {
        RuntimeFixture fixture = runtimeFixture();
        fixture.saveRule(stateAddRule());
        fixture.createInvoice("C-STATE", new BigDecimal("5"));
        DynamicRecord contract = fixture.service.newRecord("sales.contract", "contract")
                .setValue("contractNo", "C-STATE")
                .setValue("amount", BigDecimal.TEN)
                .setValue("status", "DRAFT");
        fixture.service.create("sales.contract", "contract", contract);
        String contractId = String.valueOf(fixture.table("app_contract").getFirst().get("id"));

        DynamicRecord approved = fixture.service.newRecord("sales.contract", "contract")
                .setValue("contractNo", "C-STATE")
                .setValue("amount", BigDecimal.TEN)
                .setValue("status", "APPROVED");
        approved.setId(contractId);
        fixture.service.update("sales.contract", "contract", approved);

        assertThat(fixture.table("app_invoice")).singleElement()
                .satisfies(row -> assertThat(row.get("received_amount")).isEqualTo(new BigDecimal("15")));
        String invoiceId = String.valueOf(fixture.table("app_invoice").getFirst().get("id"));
        assertThat(fixture.effectLogService.selectByTarget("finance.invoice", invoiceId, null))
                .singleElement()
                .satisfies(effect -> {
                    assertThat(effect.getOperation()).isEqualTo(RecordWriteBackFieldOperation.ADD);
                    assertThat(effect.getStatus()).isEqualTo(RecordWriteBackEffectStatus.ACTIVE);
                    assertThat(effect.getContributionValue()).isEqualTo("10");
                    assertThat(effect.getDeltaValue()).isEqualTo("10");
                    assertThat(effect.getBeforeValue()).isEqualTo("5");
                    assertThat(effect.getAfterValue()).isEqualTo("15");
                });

        DynamicRecord draft = fixture.service.newRecord("sales.contract", "contract")
                .setValue("contractNo", "C-STATE")
                .setValue("amount", BigDecimal.TEN)
                .setValue("status", "DRAFT");
        draft.setId(contractId);
        fixture.service.update("sales.contract", "contract", draft);

        assertThat(fixture.table("app_invoice")).singleElement()
                .satisfies(row -> assertThat(row.get("received_amount")).isEqualTo(new BigDecimal("5")));
        assertThat(fixture.effectLogService.selectActiveContributions(fixture.ruleService.selectEnabledRuleTrees(
                                "sales.contract",
                                net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationEventType.AFTER_SAVE)
                        .getFirst().getId(),
                "sales.contract", contractId, "finance.invoice", invoiceId, "receivedAmount", "amount"))
                .isEmpty();
    }

    private RuntimeFixture runtimeFixture() {
        return runtimeFixture(new AllowAllDataScopeCriteriaService());
    }

    private RuntimeFixture runtimeFixture(DataScopeCriteriaService dataScopeCriteriaService) {
        InMemoryDynamicTables tables = new InMemoryDynamicTables();
        IDatabaseOperations<Object> operations = tables.operations();
        AtomicReference<RecordWriteBackRuntimeService> runtimeRef = new AtomicReference<>();
        List<DynamicRecordMutationEvent> mutationEvents = new ArrayList<>();
        List<String> childLifecycleEvents = new ArrayList<>();
        DynamicRecordMutationCoordinator writeBackCoordinator = new DynamicRecordMutationCoordinator() {
            @Override
            public void afterMutation(DynamicRecordMutationEvent event) {
                mutationEvents.add(event);
                runtimeRef.get().onMutationEvent(event);
            }
        };
        DynamicRecordMutationCoordinator lifecycleRecorder = new DynamicRecordMutationCoordinator() {
            @Override
            public void beforeRelationChildUpdate(String moduleAlias,
                                                  String parentEntityAlias,
                                                  String relationCode,
                                                  String childEntityAlias,
                                                  DynamicRecord parentBefore,
                                                  DynamicRecord parentIncoming,
                                                  DynamicRecord childBefore,
                                                  DynamicRecord childIncoming) {
                childLifecycleEvents.add("before:" + relationCode + ":" + childEntityAlias + ":"
                        + childIncoming.getId());
            }

            @Override
            public void afterRelationChildUpdate(String moduleAlias,
                                                 String parentEntityAlias,
                                                 String relationCode,
                                                 String childEntityAlias,
                                                 DynamicRecord parentBefore,
                                                 DynamicRecord parentUpdated,
                                                 DynamicRecord childBefore,
                                                 DynamicRecord childUpdated) {
                childLifecycleEvents.add("after:" + relationCode + ":" + childEntityAlias + ":"
                        + childUpdated.getId());
            }
        };
        RecordImpactRelationService impactRelationService =
                new RecordImpactRelationService(new TestMemoryDao<>());
        DynamicRecordMutationCoordinator coordinator = DynamicRecordMutationCoordinators.composite(List.of(
                new RecordImpactOriginCoordinator(impactRelationService),
                lifecycleRecorder,
                writeBackCoordinator
        ));
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(
                operations,
                new DynamicModuleRegistry(),
                DynamicFieldValueValidator.NONE
        ).register(contractModule()).register(invoiceModule());
        DynamicRecordService service = new DynamicRecordService(
                runtime,
                new AllowAllActionExecutionPolicyService(),
                dataScopeCriteriaService,
                coordinator);
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackEffectLogService effectLogService =
                new RecordWriteBackEffectLogService(new TestMemoryDao<>());
        RecordWriteBackRuntimeService writeBackRuntime = new RecordWriteBackRuntimeService(
                List.of(),
                Optional.of(ruleService),
                Optional.of(new RecordWriteBackExecutionLogService(new TestMemoryDao<>())),
                Optional.of(effectLogService),
                Optional.of(impactRelationService),
                Optional.of(service));
        runtimeRef.set(writeBackRuntime);
        return new RuntimeFixture(service, ruleService, effectLogService, impactRelationService, tables, mutationEvents,
                childLifecycleEvents);
    }

    private RecordWriteBackRuleService ruleService() {
        return new RecordWriteBackRuleService(
                new TestMemoryDao<>(),
                new RecordWriteBackMatchRuleService(new TestMemoryDao<>()),
                new RecordWriteBackFieldRuleService(new TestMemoryDao<>()));
    }

    private RecordWriteBackRule baseRule() {
        RecordWriteBackRule rule = new RecordWriteBackRule();
        rule.setTriggerModuleAlias("sales.contract");
        rule.setTargetModuleAlias("finance.invoice");
        rule.setTitle("合同金额回写发票");
        rule.setCascadeMode(RecordWriteBackCascadeMode.SINGLE_HOP);
        RecordWriteBackMatchRule matchRule = new RecordWriteBackMatchRule();
        matchRule.setSourceField("contractNo");
        matchRule.setTargetField("contractNo");
        RecordWriteBackFieldRule fieldRule = new RecordWriteBackFieldRule();
        fieldRule.setSourceType(RecordWriteBackFieldSourceType.FIELD);
        fieldRule.setSourceField("amount");
        fieldRule.setTargetField("receivedAmount");
        fieldRule.setOperation(RecordWriteBackFieldOperation.COVER);
        rule.setMatchRules(List.of(matchRule));
        rule.setFieldRules(List.of(fieldRule));
        return rule;
    }

    private RecordWriteBackRule stateAddRule() {
        RecordWriteBackRule rule = baseRule();
        rule.setTitle("合同审批通过累加发票金额");
        rule.setTriggerMode(RecordWriteBackTriggerMode.ON_ENTER);
        rule.setTriggerModes("ON_ENTER,ON_EXIT,ON_CHANGE_WHILE_EFFECTIVE");
        rule.setTriggerField("status");
        rule.setTriggerValue("APPROVED");
        rule.getFieldRules().getFirst().setOperation(RecordWriteBackFieldOperation.ADD);
        return rule;
    }

    private RecordWriteBackRule relationRule() {
        RecordWriteBackRule rule = new RecordWriteBackRule();
        rule.setTriggerModuleAlias("finance.invoice");
        rule.setTargetModuleAlias("sales.contract");
        rule.setTitle("发票通过生成关系回写合同");
        rule.setTargetLocateMode(RecordWriteBackTargetLocateMode.GENERATION_RELATION);
        rule.setRelationGenerationRuleId("gen-1");
        rule.setCascadeMode(RecordWriteBackCascadeMode.SINGLE_HOP);
        RecordWriteBackFieldRule fieldRule = new RecordWriteBackFieldRule();
        fieldRule.setSourceType(RecordWriteBackFieldSourceType.FIELD);
        fieldRule.setSourceField("receivedAmount");
        fieldRule.setTargetField("amount");
        fieldRule.setOperation(RecordWriteBackFieldOperation.COVER);
        rule.setMatchRules(List.of());
        rule.setFieldRules(List.of(fieldRule));
        return rule;
    }

    private RecordWriteBackRule childLineRule() {
        RecordWriteBackRule rule = baseRule();
        rule.setTitle("合同金额回写发票分录");
        rule.setTargetRelationCode("lines");
        rule.setTargetEntityAlias("invoice_line");
        RecordWriteBackMatchRule childMatchRule = new RecordWriteBackMatchRule();
        childMatchRule.setSourceField("contractNo");
        childMatchRule.setTargetField("lineNo");
        childMatchRule.setTargetRelationCode("lines");
        rule.setMatchRules(List.of(rule.getMatchRules().getFirst(), childMatchRule));
        return rule;
    }

    private RecordOriginContext originContext(String sourceRecordId, String batchId, String draftKey) {
        return new RecordOriginContext(
                RecordImpactType.GENERATE_PUSH,
                "sales.contract",
                sourceRecordId,
                "finance.invoice",
                "gen-1",
                "pushInvoice",
                batchId,
                draftKey
        );
    }

    private ModuleDefinition contractModule() {
        return new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(new EntityDefinition("contract", "app_contract", "Contract", List.of(
                        FieldDefinition.string("contractNo", "Contract No").column("contract_no"),
                        FieldDefinition.string("status", "Status"),
                        FieldDefinition.decimal("amount", "Amount").precision(18, 2)
                )))
        );
    }

    private ModuleDefinition invoiceModule() {
        return new ModuleDefinition(
                "finance.invoice",
                "Invoice",
                List.of(
                        new EntityDefinition("invoice", "app_invoice", "Invoice", List.of(
                                FieldDefinition.string("contractNo", "Contract No").column("contract_no"),
                                FieldDefinition.decimal("receivedAmount", "Received Amount")
                                        .column("received_amount")
                                        .precision(18, 2)
                        )),
                        new EntityDefinition("invoice_line", "app_invoice_line", "Invoice Line", List.of(
                                FieldDefinition.string("invoiceId", "Invoice Id").column("invoice_id"),
                                FieldDefinition.string("lineNo", "Line No").column("line_no"),
                                FieldDefinition.decimal("receivedAmount", "Received Amount")
                                        .column("received_amount")
                                        .precision(18, 2)
                        ))
                ),
                List.of(EntityRelationDefinition.child("lines", "invoice", "invoice_line", "invoiceId"))
        );
    }

    private record RuntimeFixture(DynamicRecordService service,
                                  RecordWriteBackRuleService ruleService,
                                  RecordWriteBackEffectLogService effectLogService,
                                  RecordImpactRelationService impactRelationService,
                                  InMemoryDynamicTables tables,
                                  List<DynamicRecordMutationEvent> mutationEvents,
                                  List<String> childLifecycleEvents) {
        void saveRule(RecordWriteBackRule rule) {
            ruleService.saveRuleTree(rule);
        }

        void createInvoice(String contractNo, BigDecimal receivedAmount) {
            DynamicRecord invoice = service.newRecord("finance.invoice", "invoice")
                    .setValue("contractNo", contractNo)
                    .setValue("receivedAmount", receivedAmount);
            service.create("finance.invoice", "invoice", invoice);
        }

        List<Map<String, Object>> table(String tableName) {
            return tables.table(tableName);
        }
    }

    private static final class HideFinanceInvoiceDataScopeCriteriaService implements DataScopeCriteriaService {
        @Override
        public DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                                        String actionCode,
                                                        Criteria criteria,
                                                        Optional<CurrentUser> currentUser) {
            Criteria scoped = criteria == null ? Criteria.of() : criteria;
            if ("finance.invoice".equals(moduleAlias)) {
                scoped.eq("id", "not-visible");
            }
            return DataScopeCriteriaResult.restricted(scoped);
        }

        @Override
        public DataScopeCriteriaResult resolveReadScope(String moduleAlias,
                                                        ActionExecutionPolicy policy,
                                                        Criteria criteria,
                                                        Optional<CurrentUser> currentUser) {
            return resolveReadScope(moduleAlias, policy.permissionActionCode(), criteria, currentUser);
        }
    }

    private static final class InMemoryDynamicTables {
        private final Map<String, List<Map<String, Object>>> rows = new LinkedHashMap<>();
        private final Map<String, Integer> sequences = new LinkedHashMap<>();

        @SuppressWarnings("unchecked")
        IDatabaseOperations<Object> operations() {
            IDatabaseOperations<Object> operations = mock(IDatabaseOperations.class);
            when(operations.getDBInfo()).thenReturn(new DBInfo("POSTGRESQL").setName("muyun_test"));
            when(operations.getDefaultSchemaName()).thenReturn("public");
            when(operations.insertItem(anyString(), anyString(), anyMap()))
                    .thenAnswer(invocation -> insert(invocation.getArgument(1), invocation.getArgument(2)));
            when(operations.query(anyString(), anyMap()))
                    .thenAnswer(invocation -> query(invocation.getArgument(0), invocation.getArgument(1)));
            when(operations.row(anyString(), anyMap()))
                    .thenAnswer(invocation -> Map.of("total_count", query(invocation.getArgument(0),
                            invocation.getArgument(1)).size()));
            when(operations.patchUpdateItemWhere(anyString(), anyString(), anyMap(), anyMap()))
                    .thenAnswer(invocation -> update(invocation.getArgument(1), invocation.getArgument(2),
                            invocation.getArgument(3)));
            return operations;
        }

        List<Map<String, Object>> table(String tableName) {
            return rows.getOrDefault(tableName, List.of()).stream()
                    .map(LinkedHashMap::new)
                    .map(row -> (Map<String, Object>) row)
                    .toList();
        }

        private String insert(String tableName, Map<String, Object> body) {
            String id = body.get("id") == null || body.get("id").toString().isBlank()
                    ? nextId(tableName)
                    : body.get("id").toString();
            Map<String, Object> row = new LinkedHashMap<>(body);
            row.put("id", id);
            row.putIfAbsent("deleted", Boolean.FALSE);
            rows.computeIfAbsent(tableName, ignored -> new ArrayList<>()).add(row);
            return id;
        }

        private List<Map<String, Object>> query(String sql, Map<String, Object> params) {
            String tableName = tableName(sql);
            List<Map<String, Object>> candidates = rows.getOrDefault(tableName, List.of()).stream()
                    .filter(row -> matches(row, params))
                    .map(LinkedHashMap::new)
                    .map(row -> (Map<String, Object>) row)
                    .toList();
            return candidates;
        }

        private int update(String tableName, Map<String, Object> body, Map<String, Object> where) {
            for (Map<String, Object> row : rows.getOrDefault(tableName, List.of())) {
                if (matches(row, where)) {
                    row.putAll(body);
                    return 1;
                }
            }
            return 0;
        }

        private boolean matches(Map<String, Object> row, Map<String, Object> params) {
            if (params == null || params.isEmpty()) {
                return true;
            }
            for (Object value : params.values()) {
                if (value == null || value instanceof Number) {
                    continue;
                }
                if (value instanceof Boolean && !row.containsValue(value)) {
                    return false;
                }
                if (value instanceof String text && !text.isBlank()) {
                    if (text.equals(row.get("id")) || text.equals(row.get("contract_no"))
                            || text.equals(row.get("invoice_id")) || text.equals(row.get("line_no"))
                            || text.equals(row.get("tenant_id"))) {
                        continue;
                    }
                    return false;
                }
            }
            return true;
        }

        private String tableName(String sql) {
            if (sql.contains("app_invoice_line")) {
                return "app_invoice_line";
            }
            if (sql.contains("app_invoice")) {
                return "app_invoice";
            }
            if (sql.contains("app_contract")) {
                return "app_contract";
            }
            throw new IllegalArgumentException("unknown test table in SQL: " + sql);
        }

        private String nextId(String tableName) {
            int next = sequences.merge(tableName, 1, Integer::sum);
            return tableName.replace("app_", "") + "-" + next;
        }
    }
}
