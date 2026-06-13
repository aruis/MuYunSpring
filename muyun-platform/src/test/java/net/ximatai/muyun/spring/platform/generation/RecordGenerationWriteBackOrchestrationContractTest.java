package net.ximatai.muyun.spring.platform.generation;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.common.platform.AllowAllActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.AllowAllDataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceAffectDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceFilterDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionResult;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutorRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicFieldValueValidator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicModuleRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationCoordinator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationCoordinators;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationEvent;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveRequest;
import net.ximatai.muyun.spring.platform.impact.RecordImpactOriginCoordinator;
import net.ximatai.muyun.spring.platform.impact.RecordImpactRelationService;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackCascadeMode;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackEffectLogService;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackExecutionLogService;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackFieldOperation;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackFieldRule;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackFieldRuleService;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackFieldSourceType;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackMatchRuleService;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackRule;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackRuleService;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackRuntimeService;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackTargetLocateMode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecordGenerationWriteBackOrchestrationContractTest {
    private static final String GENERATION_RULE_ID = "generate-contract-invoice";

    @Test
    void shouldGenerateTargetDraftSaveOriginRelationAndWriteBackSourceRecord() {
        RuntimeFixture fixture = runtimeFixture();
        DynamicRecord contract = fixture.service.newRecord("sales.contract", "contract")
                .setValue("title", "C-GEN")
                .setValue("contractNo", "C-GEN")
                .setValue("amount", new BigDecimal("100.00"));
        fixture.service.create("sales.contract", "contract", contract);
        String contractId = String.valueOf(fixture.table("app_contract").getFirst().get("id"));

        RecordGenerationRule generationRule = fixture.generationRuleService.saveRuleTree(generationRule());
        fixture.writeBackRuleService.saveRuleTree(relationRule(generationRule.getId()));

        DynamicActionExecutionResult result = fixture.service.entity("sales.contract", "contract")
                .executeAction("generateInvoice", DynamicActionExecutionRequest.id(contractId));
        RecordGenerationResult generation = (RecordGenerationResult) result.value();
        RecordGenerationDraft draft = generation.drafts().getFirst();
        DynamicRecord resetContract = fixture.service.newRecord("sales.contract", "contract")
                .setValue("title", "C-GEN")
                .setValue("contractNo", "C-GEN")
                .setValue("amount", BigDecimal.ZERO);
        resetContract.setId(contractId);
        fixture.service.update("sales.contract", "contract", resetContract);
        ReferenceRecordGenerationFacade facade = new ReferenceRecordGenerationFacade(
                fixture.service, fixture.generationRuleService);
        String invoiceId = facade.confirmDraft(draft);

        assertThat(fixture.impactRelationService.hasGeneratedTarget(
                "sales.contract", contractId, "finance.invoice", generationRule.getId())).isTrue();
        assertThat(fixture.impactRelationService.listGeneratedTargets(
                        "sales.contract", contractId, "finance.invoice", generationRule.getId(), null))
                .singleElement()
                .satisfies(relation -> {
                    assertThat(relation.getBatchId()).isEqualTo(generation.batchId());
                    assertThat(relation.getDraftKey()).isEqualTo("invoice:1");
                    assertThat(relation.getTargetRecordId()).isEqualTo(invoiceId);
                });
        assertThat(fixture.table("app_contract")).singleElement()
                .satisfies(row -> assertThat(row.get("amount")).isEqualTo(new BigDecimal("100.00")));
        assertThat(fixture.effectLogService.selectByTarget("sales.contract", contractId, null))
                .singleElement()
                .satisfies(effect -> {
                    assertThat(effect.getTriggerModuleAlias()).isEqualTo("finance.invoice");
                    assertThat(effect.getTargetField()).isEqualTo("amount");
                    assertThat(effect.getAfterValue()).isEqualTo("100.00");
                });
    }

    @Test
    void shouldConfirmAllGeneratedDraftsAndReturnCommittedRecordIds() {
        RuntimeFixture fixture = runtimeFixture();
        DynamicRecord contract = fixture.service.newRecord("sales.contract", "contract")
                .setValue("title", "C-GEN")
                .setValue("contractNo", "C-GEN")
                .setValue("amount", new BigDecimal("100.00"));
        fixture.service.create("sales.contract", "contract", contract);
        String contractId = String.valueOf(fixture.table("app_contract").getFirst().get("id"));

        RecordGenerationRule generationRule = fixture.generationRuleService.saveRuleTree(generationRule());
        DynamicActionExecutionResult result = fixture.service.entity("sales.contract", "contract")
                .executeAction("generateInvoice", DynamicActionExecutionRequest.id(contractId));
        RecordGenerationResult generation = (RecordGenerationResult) result.value();
        ReferenceRecordGenerationFacade facade = new ReferenceRecordGenerationFacade(
                fixture.service, fixture.generationRuleService);

        RecordGenerationCommitResult commit = facade.confirmAll(generation);

        assertThat(commit.ruleId()).isEqualTo(generationRule.getId());
        assertThat(commit.batchId()).isEqualTo(generation.batchId());
        assertThat(commit.targetModuleAlias()).isEqualTo("finance.invoice");
        assertThat(commit.recordIds()).singleElement()
                .isEqualTo(String.valueOf(fixture.table("app_invoice").getFirst().get("id")));
    }

    @Test
    void shouldResolveReferenceGenerateDraftAndConfirmOriginRelation() {
        RuntimeFixture fixture = runtimeFixture();
        DynamicRecord contract = fixture.service.newRecord("sales.contract", "contract")
                .setValue("title", "C-REF")
                .setValue("contractNo", "C-REF")
                .setValue("region", "north")
                .setValue("amount", new BigDecimal("200.00"));
        fixture.service.create("sales.contract", "contract", contract);
        String contractId = String.valueOf(fixture.table("app_contract").getFirst().get("id"));
        RecordGenerationRule generationRule = fixture.generationRuleService.saveRuleTree(generationRule());

        var candidates = fixture.service.resolveReference(
                "finance.invoice",
                "invoice",
                "contractId",
                DynamicReferenceResolveRequest.query(null)
                        .withFormValues(Map.of("contractRegion", "north"))
        );

        assertThat(candidates.options()).singleElement()
                .satisfies(option -> {
                    assertThat(option.id()).isEqualTo(contractId);
                    assertThat(option.title()).isEqualTo("C-REF");
                    assertThat(option.affectPatch()).containsEntry("contractRegion", "north");
                });
        ReferenceRecordGenerationFacade facade = new ReferenceRecordGenerationFacade(
                fixture.service, fixture.generationRuleService);
        RecordGenerationResult generation = facade.generateFromReference(
                "finance.invoice",
                "invoice",
                "contractId",
                contractId);
        RecordGenerationCommitResult commit = facade.confirmAll(generation);

        assertThat(generation.ruleId()).isEqualTo(generationRule.getId());
        assertThat(generation.drafts()).singleElement()
                .satisfies(draft -> assertThat(draft.record().getValue("receivedAmount"))
                        .isEqualTo(new BigDecimal("200.00")));
        assertThat(commit.recordIds()).singleElement()
                .isEqualTo(String.valueOf(fixture.table("app_invoice").getFirst().get("id")));
        assertThat(fixture.impactRelationService.hasGeneratedTarget(
                "sales.contract", contractId, "finance.invoice", generationRule.getId())).isTrue();
    }

    private RuntimeFixture runtimeFixture() {
        InMemoryDynamicTables tables = new InMemoryDynamicTables();
        IDatabaseOperations<Object> operations = tables.operations();
        RecordGenerationRuleService generationRuleService = generationRuleService();
        RecordWriteBackRuleService writeBackRuleService = writeBackRuleService();
        RecordWriteBackEffectLogService effectLogService = new RecordWriteBackEffectLogService(new TestMemoryDao<>());
        RecordImpactRelationService impactRelationService = new RecordImpactRelationService(new TestMemoryDao<>());
        AtomicReference<RecordWriteBackRuntimeService> runtimeRef = new AtomicReference<>();
        DynamicRecordMutationCoordinator writeBackCoordinator = new DynamicRecordMutationCoordinator() {
            @Override
            public void afterMutation(DynamicRecordMutationEvent event) {
                runtimeRef.get().onMutationEvent(event);
            }
        };
        DynamicRecordMutationCoordinator coordinator = DynamicRecordMutationCoordinators.composite(List.of(
                new RecordImpactOriginCoordinator(impactRelationService),
                writeBackCoordinator
        ));
        DynamicActionExecutorRegistry executors = new DynamicActionExecutorRegistry(
                List.of(new RecordGenerationActionExecutor(generationRuleService)));
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(
                operations,
                new DynamicModuleRegistry(),
                DynamicFieldValueValidator.NONE,
                null,
                executors
        ).register(contractModule()).register(invoiceModule());
        DynamicRecordService service = new DynamicRecordService(
                runtime,
                new AllowAllActionExecutionPolicyService(),
                new AllowAllDataScopeCriteriaService(),
                coordinator);
        RecordWriteBackRuntimeService writeBackRuntime = new RecordWriteBackRuntimeService(
                List.of(),
                Optional.of(writeBackRuleService),
                Optional.of(new RecordWriteBackExecutionLogService(new TestMemoryDao<>())),
                Optional.of(effectLogService),
                Optional.of(impactRelationService),
                Optional.of(service));
        runtimeRef.set(writeBackRuntime);
        return new RuntimeFixture(service, generationRuleService, writeBackRuleService, effectLogService,
                impactRelationService, tables);
    }

    private RecordGenerationRule generationRule() {
        RecordGenerationRule rule = new RecordGenerationRule();
        rule.setId(GENERATION_RULE_ID);
        rule.setSourceModuleAlias("sales.contract");
        rule.setTargetModuleAlias("finance.invoice");
        rule.setActionCode("generateInvoice");
        rule.setTitle("生成发票");
        rule.setEnabled(Boolean.TRUE);
        RecordGenerationObjectMapping mapping = new RecordGenerationObjectMapping();
        mapping.setSourceObjectAlias("contract");
        mapping.setTargetObjectAlias("invoice");
        mapping.setFieldMappings(List.of(
                fieldMapping("contractNo", "contractNo"),
                fieldMapping("receivedAmount", "amount")
        ));
        rule.setObjectMappings(List.of(mapping));
        return rule;
    }

    private RecordGenerationFieldMapping fieldMapping(String targetField, String sourceField) {
        RecordGenerationFieldMapping mapping = new RecordGenerationFieldMapping();
        mapping.setTargetField(targetField);
        mapping.setSourceField(sourceField);
        mapping.setMappingType(RecordGenerationFieldSourceType.DIRECT);
        return mapping;
    }

    private RecordWriteBackRule relationRule(String generationRuleId) {
        RecordWriteBackRule rule = new RecordWriteBackRule();
        rule.setTriggerModuleAlias("finance.invoice");
        rule.setTargetModuleAlias("sales.contract");
        rule.setTitle("发票通过生成关系回写合同");
        rule.setTargetLocateMode(RecordWriteBackTargetLocateMode.GENERATION_RELATION);
        rule.setRelationGenerationRuleId(generationRuleId);
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

    private RecordGenerationRuleService generationRuleService() {
        return new RecordGenerationRuleService(
                new TestMemoryDao<>(),
                new RecordGenerationObjectMappingService(new TestMemoryDao<>()),
                new RecordGenerationFieldMappingService(new TestMemoryDao<>()),
                new RecordGenerationSplitPolicyService(new TestMemoryDao<>()),
                new RecordGenerationSplitGroupFieldService(new TestMemoryDao<>()));
    }

    private RecordWriteBackRuleService writeBackRuleService() {
        return new RecordWriteBackRuleService(
                new TestMemoryDao<>(),
                new RecordWriteBackMatchRuleService(new TestMemoryDao<>()),
                new RecordWriteBackFieldRuleService(new TestMemoryDao<>()));
    }

    private ModuleDefinition contractModule() {
        return new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(new EntityDefinition("contract", "app_contract", "Contract", List.of(
                        FieldDefinition.titleField(),
                        FieldDefinition.string("contractNo", "Contract No").column("contract_no"),
                        FieldDefinition.string("region", "Region"),
                        FieldDefinition.decimal("amount", "Amount").precision(18, 2)
                )).withCapabilities(EntityCapability.DATA_SCOPE, EntityCapability.REFERENCE)),
                List.of(),
                List.of(),
                List.of(),
                List.of(new EntityActionDefinition(
                        "contract",
                        "generateInvoice",
                        "生成发票",
                        true,
                        EntityActionLevel.RECORD,
                        EntityActionCategory.GENERATE,
                        EntityActionAccessMode.AUTH_REQUIRED,
                        true,
                        true,
                        null,
                        null,
                        null,
                        EntityActionExecutorType.GENERATE,
                        RecordGenerationActionExecutor.EXECUTOR_KEY
                ))
        );
    }

    private ModuleDefinition invoiceModule() {
        return new ModuleDefinition(
                "finance.invoice",
                "Invoice",
                List.of(new EntityDefinition("invoice", "app_invoice", "Invoice", List.of(
                        FieldDefinition.string("contractId", "Contract").column("contract_id"),
                        FieldDefinition.string("contractRegion", "Contract Region").column("contract_region"),
                        FieldDefinition.string("contractNo", "Contract No").column("contract_no"),
                        FieldDefinition.decimal("receivedAmount", "Received Amount")
                                .column("received_amount")
                                .precision(18, 2)
                ))),
                List.of(),
                List.of(EntityReferenceDefinition.to("invoice", "contractId",
                                ReferenceTarget.of("sales.contract", "contract"))
                        .withRuntimeConfig("id", "contractNo", GENERATION_RULE_ID, "contract-query",
                                java.util.Set.of("region"))
                        .withInteractionRules(
                                List.of(new EntityReferenceFilterDefinition(
                                        "contractRegion", "region", DynamicQueryOperator.EQ)),
                                List.of(new EntityReferenceAffectDefinition("region", "contractRegion"))
                        ))
        );
    }

    private record RuntimeFixture(DynamicRecordService service,
                                  RecordGenerationRuleService generationRuleService,
                                  RecordWriteBackRuleService writeBackRuleService,
                                  RecordWriteBackEffectLogService effectLogService,
                                  RecordImpactRelationService impactRelationService,
                                  InMemoryDynamicTables tables) {
        List<Map<String, Object>> table(String tableName) {
            return tables.table(tableName);
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
            when(operations.insertItem(anyString(), anyString(), anyMap(), anyString()))
                    .thenAnswer(invocation -> insert(invocation.getArgument(1), invocation.getArgument(2)));
            when(operations.query(anyString(), anyMap()))
                    .thenAnswer(invocation -> query(invocation.getArgument(0), invocation.getArgument(1)));
            when(operations.row(anyString(), anyMap()))
                    .thenAnswer(invocation -> Map.of("total_count", query(invocation.getArgument(0),
                            invocation.getArgument(1)).size()));
            when(operations.patchUpdateItemWhere(anyString(), anyString(), anyMap(), anyMap(), anyString()))
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
            return rows.getOrDefault(tableName, List.of()).stream()
                    .filter(row -> matches(row, params))
                    .map(LinkedHashMap::new)
                    .map(row -> (Map<String, Object>) row)
                    .toList();
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
                if (value instanceof String text && !text.isBlank() && !row.containsValue(text)) {
                    return false;
                }
            }
            return true;
        }

        private String tableName(String sql) {
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
