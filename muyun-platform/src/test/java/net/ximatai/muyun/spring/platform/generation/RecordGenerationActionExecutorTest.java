package net.ximatai.muyun.spring.platform.generation;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionResult;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutorRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionResultType;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicFieldValueValidator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicModuleRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.impact.RecordImpactType;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecordGenerationActionExecutorTest {
    private final IDatabaseOperations<Object> operations = operations();
    private final RecordingPolicyService policyService = new RecordingPolicyService();
    private final RecordGenerationRuleService ruleService = ruleService();

    @Test
    void shouldGenerateTargetDraftsWithOriginContextThroughDynamicAction() {
        stubSourceQueries();
        RecordGenerationRule rule = generationRule();
        RecordGenerationObjectMapping mapping = objectMapping(
                fieldMapping("contractNo", "opportunityNo"),
                constantMapping("status", "draft"),
                formulaMapping("amountWithTax", "{amount} * 1.06"));
        rule.setObjectMappings(List.of(mapping));
        ruleService.saveRuleTree(rule);
        DynamicRecordService recordService = recordService();

        DynamicActionExecutionResult result = recordService.entity("sales.opportunity", "opportunity")
                .executeAction("generateContract", DynamicActionExecutionRequest.id("opp-1"));

        assertThat(result.body().type()).isEqualTo(DynamicActionResultType.OBJECT);
        assertThat(result.value()).isInstanceOf(RecordGenerationResult.class);
        RecordGenerationResult generation = (RecordGenerationResult) result.value();
        assertThat(generation.sourceModuleAlias()).isEqualTo("sales.opportunity");
        assertThat(generation.sourceRecordId()).isEqualTo("opp-1");
        assertThat(generation.targetModuleAlias()).isEqualTo("sales.contract");
        assertThat(generation.drafts()).singleElement().satisfies(draft -> {
            assertThat(draft.targetModuleAlias()).isEqualTo("sales.contract");
            assertThat(draft.targetEntityAlias()).isEqualTo("contract");
            assertThat(draft.record().getId()).isNull();
            assertThat(draft.record().getValue("contractNo")).isEqualTo("OPP-001");
            assertThat(draft.record().getValue("status")).isEqualTo("draft");
            assertThat(new BigDecimal(draft.record().getValue("amountWithTax").toString()))
                    .isEqualByComparingTo("106.00");
            assertThat(draft.originContext().impactType()).isEqualTo(RecordImpactType.GENERATE_PUSH);
            assertThat(draft.originContext().sourceModuleAlias()).isEqualTo("sales.opportunity");
            assertThat(draft.originContext().sourceRecordId()).isEqualTo("opp-1");
            assertThat(draft.originContext().targetModuleAlias()).isEqualTo("sales.contract");
            assertThat(draft.originContext().generationRuleId()).isEqualTo(rule.getId());
            assertThat(draft.originContext().actionCode()).isEqualTo("generateContract");
            assertThat(draft.originContext().batchId()).isEqualTo(generation.batchId());
        });
        assertThat(policyService.actions())
                .contains("sales.opportunity:generateContract:[opp-1]", "sales.contract:create:[]");
    }

    @Test
    void shouldGenerateTargetDraftsFromReferenceFieldFacade() {
        stubSourceQueries();
        RecordGenerationRule rule = generationRule();
        rule.setObjectMappings(List.of(objectMapping(fieldMapping("contractNo", "opportunityNo"))));
        ruleService.saveRuleTree(rule);
        DynamicRecordService recordService = recordServiceWithReference(rule);
        ReferenceRecordGenerationFacade facade = new ReferenceRecordGenerationFacade(recordService, ruleService);

        RecordGenerationResult generation = facade.generateFromReference(
                "sales.contract",
                "contract",
                "opportunityId",
                "opp-1"
        );

        assertThat(generation.ruleId()).isEqualTo(rule.getId());
        assertThat(generation.sourceModuleAlias()).isEqualTo("sales.opportunity");
        assertThat(generation.sourceRecordId()).isEqualTo("opp-1");
        assertThat(generation.targetModuleAlias()).isEqualTo("sales.contract");
        assertThat(generation.drafts()).singleElement()
                .satisfies(draft -> assertThat(draft.record().getValue("contractNo")).isEqualTo("OPP-001"));
        assertThat(policyService.actions())
                .contains("sales.opportunity:generateContract:[opp-1]", "sales.contract:create:[]");
    }

    @Test
    void shouldGenerateRootDraftWithChildRows() {
        stubSourceQueries();
        RecordGenerationRule rule = generationRule();
        RecordGenerationObjectMapping root = objectMapping(fieldMapping("contractNo", "opportunityNo"));
        RecordGenerationObjectMapping line = objectMapping(fieldMapping("itemName", "productName"),
                fieldMapping("lineAmount", "lineAmount"));
        line.setSourceObjectAlias("opportunity_line");
        line.setSourceRelationCode("lines");
        line.setTargetObjectAlias("contract_line");
        line.setTargetRelationCode("lines");
        rule.setObjectMappings(List.of(root, line));
        ruleService.saveRuleTree(rule);
        DynamicRecordService recordService = recordService();

        DynamicActionExecutionResult result = recordService.entity("sales.opportunity", "opportunity")
                .executeAction("generateContract", DynamicActionExecutionRequest.id("opp-1"));

        RecordGenerationResult generation = (RecordGenerationResult) result.value();
        assertThat(generation.drafts()).singleElement().satisfies(draft -> {
            assertThat(draft.record().getValue("contractNo")).isEqualTo("OPP-001");
            assertThat(draft.record().getChildren("lines")).hasSize(2);
            assertThat(draft.record().getChildren("lines")).extracting(lineDraft -> lineDraft.getValue("itemName"))
                    .containsExactly("服务A", "服务B");
            assertThat(draft.record().getChildren("lines")).extracting(lineDraft -> lineDraft.getValue("lineAmount"))
                    .containsExactly(new BigDecimal("60.00"), new BigDecimal("40.00"));
            assertThat(draft.originContext().draftKey()).isEqualTo("contract:1");
        });
    }

    @Test
    void shouldRejectRootObjectSplitDriver() {
        stubSourceQueries();
        RecordGenerationRule rule = generationRule();
        RecordGenerationObjectMapping mapping = objectMapping(fieldMapping("contractNo", "opportunityNo"));
        mapping.setSplitDriver(Boolean.TRUE);
        rule.setObjectMappings(List.of(mapping));
        ruleService.saveRuleTree(rule);
        DynamicRecordService recordService = recordService();

        assertThatThrownBy(() -> recordService.entity("sales.opportunity", "opportunity")
                .executeAction("generateContract", DynamicActionExecutionRequest.id("opp-1")))
                .hasMessageContaining("split driver must be child object mapping");
    }

    @Test
    void shouldSplitRootDraftsByLoadedChildRows() {
        stubSourceQueries();
        RecordGenerationRule rule = generationRule();
        RecordGenerationObjectMapping root = objectMapping(fieldMapping("contractNo", "opportunityNo"),
                constantMapping("status", "draft"));
        RecordGenerationObjectMapping lineDriver = objectMapping(fieldMapping("amountWithTax", "lineAmount"));
        lineDriver.setSourceObjectAlias("opportunity_line");
        lineDriver.setSourceRelationCode("lines");
        lineDriver.setTargetObjectAlias("contract");
        lineDriver.setSplitDriver(Boolean.TRUE);
        rule.setObjectMappings(List.of(root, lineDriver));
        ruleService.saveRuleTree(rule);
        DynamicRecordService recordService = recordService();

        DynamicActionExecutionResult result = recordService.entity("sales.opportunity", "opportunity")
                .executeAction("generateContract", DynamicActionExecutionRequest.id("opp-1"));

        RecordGenerationResult generation = (RecordGenerationResult) result.value();
        assertThat(generation.drafts()).hasSize(2);
        assertThat(generation.drafts()).extracting(draft -> draft.originContext().draftKey())
                .containsExactly("contract:1", "contract:2");
        assertThat(generation.drafts()).extracting(draft -> draft.record().getValue("contractNo"))
                .containsExactly("OPP-001", "OPP-001");
        assertThat(generation.drafts()).extracting(draft -> draft.record().getValue("status"))
                .containsExactly("draft", "draft");
        assertThat(generation.drafts()).extracting(draft -> draft.record().getValue("amountWithTax"))
                .containsExactly(new BigDecimal("60.00"), new BigDecimal("40.00"));
    }

    @Test
    void shouldSplitRootDraftsWithOneTargetChildRowPerSourceChildRow() {
        stubSourceQueries();
        RecordGenerationRule rule = generationRule();
        RecordGenerationObjectMapping root = objectMapping(fieldMapping("contractNo", "opportunityNo"));
        RecordGenerationObjectMapping lineDriver = objectMapping(fieldMapping("itemName", "productName"),
                fieldMapping("lineAmount", "lineAmount"));
        lineDriver.setSourceObjectAlias("opportunity_line");
        lineDriver.setSourceRelationCode("lines");
        lineDriver.setTargetObjectAlias("contract_line");
        lineDriver.setTargetRelationCode("lines");
        lineDriver.setSplitDriver(Boolean.TRUE);
        rule.setObjectMappings(List.of(root, lineDriver));
        ruleService.saveRuleTree(rule);
        DynamicRecordService recordService = recordService();

        DynamicActionExecutionResult result = recordService.entity("sales.opportunity", "opportunity")
                .executeAction("generateContract", DynamicActionExecutionRequest.id("opp-1"));

        RecordGenerationResult generation = (RecordGenerationResult) result.value();
        assertThat(generation.drafts()).hasSize(2);
        assertThat(generation.drafts()).extracting(draft -> draft.originContext().draftKey())
                .containsExactly("contract:1", "contract:2");
        assertThat(generation.drafts()).allSatisfy(draft ->
                assertThat(draft.record().getChildren("lines")).singleElement()
                        .satisfies(lineDraft -> assertThat(lineDraft.getValue("lineAmount")).isNotNull()));
        assertThat(generation.drafts()).extracting(draft ->
                        draft.record().getChildren("lines").getFirst().getValue("itemName"))
                .containsExactly("服务A", "服务B");
    }

    @Test
    void shouldRejectSplitPolicyRuntimeUntilQuantityAndGroupSemanticsAreSupported() {
        stubSourceQueries();
        RecordGenerationRule rule = generationRule();
        RecordGenerationObjectMapping root = objectMapping(fieldMapping("contractNo", "opportunityNo"));
        RecordGenerationObjectMapping lineDriver = objectMapping(fieldMapping("amountWithTax", "lineAmount"));
        lineDriver.setSourceObjectAlias("opportunity_line");
        lineDriver.setSourceRelationCode("lines");
        lineDriver.setTargetObjectAlias("contract");
        lineDriver.setSplitDriver(Boolean.TRUE);
        RecordGenerationSplitPolicy splitPolicy = new RecordGenerationSplitPolicy();
        splitPolicy.setQuantityField("quantity");
        lineDriver.setSplitPolicy(splitPolicy);
        rule.setObjectMappings(List.of(root, lineDriver));
        ruleService.saveRuleTree(rule);
        DynamicRecordService recordService = recordService();

        assertThatThrownBy(() -> recordService.entity("sales.opportunity", "opportunity")
                .executeAction("generateContract", DynamicActionExecutionRequest.id("opp-1")))
                .hasMessageContaining("split policy runtime is not supported yet");
    }

    @Test
    void shouldIgnoreForgedRequestRecordValuesAndLoadPersistedSourceRecord() {
        stubSourceQueries();
        RecordGenerationRule rule = generationRule();
        rule.setObjectMappings(List.of(objectMapping(fieldMapping("contractNo", "opportunityNo"))));
        ruleService.saveRuleTree(rule);
        DynamicRecordService recordService = recordService();
        DynamicRecord forged = recordService.newRecord("sales.opportunity", "opportunity")
                .setValue("opportunityNo", "FORGED")
                .setValue("amount", new BigDecimal("999.00"));
        forged.setId("opp-1");

        DynamicActionExecutionResult result = recordService.entity("sales.opportunity", "opportunity")
                .executeAction("generateContract", DynamicActionExecutionRequest.record(forged));

        RecordGenerationResult generation = (RecordGenerationResult) result.value();
        assertThat(generation.drafts()).singleElement()
                .extracting(draft -> draft.record().getValue("contractNo"))
                .isEqualTo("OPP-001");
    }

    @Test
    void shouldReturnNoDraftWhenGenerationConditionDoesNotMatch() {
        stubSourceQueries();
        RecordGenerationRule rule = generationRule();
        rule.setGenerationCondition("{amount} > 1000");
        rule.setObjectMappings(List.of(objectMapping(fieldMapping("contractNo", "opportunityNo"))));
        ruleService.saveRuleTree(rule);
        DynamicRecordService recordService = recordService();

        DynamicActionExecutionResult result = recordService.entity("sales.opportunity", "opportunity")
                .executeAction("generateContract", DynamicActionExecutionRequest.id("opp-1"));

        RecordGenerationResult generation = (RecordGenerationResult) result.value();
        assertThat(generation.drafts()).isEmpty();
        assertThat(policyService.actions())
                .contains("sales.opportunity:generateContract:[opp-1]")
                .doesNotContain("sales.contract:create:[]");
    }

    @Test
    void shouldRejectSourceObjectAliasMismatchAtRuntime() {
        stubSourceQueries();
        RecordGenerationRule rule = generationRule();
        RecordGenerationObjectMapping mapping = objectMapping(fieldMapping("contractNo", "opportunityNo"));
        mapping.setSourceObjectAlias("other");
        rule.setObjectMappings(List.of(mapping));
        ruleService.saveRuleTree(rule);
        DynamicRecordService recordService = recordService();

        assertThatThrownBy(() -> recordService.entity("sales.opportunity", "opportunity")
                .executeAction("generateContract", DynamicActionExecutionRequest.id("opp-1")))
                .hasMessageContaining("sourceObjectAlias mismatch");
    }

    private RecordGenerationRuleService ruleService() {
        return new RecordGenerationRuleService(
                new TestMemoryDao<>(),
                new RecordGenerationObjectMappingService(new TestMemoryDao<>()),
                new RecordGenerationFieldMappingService(new TestMemoryDao<>()),
                new RecordGenerationSplitPolicyService(new TestMemoryDao<>()),
                new RecordGenerationSplitGroupFieldService(new TestMemoryDao<>()));
    }

    private DynamicRecordService recordService() {
        DynamicActionExecutorRegistry executors = new DynamicActionExecutorRegistry(
                List.of(new RecordGenerationActionExecutor(ruleService)));
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(
                operations,
                new DynamicModuleRegistry(),
                DynamicFieldValueValidator.NONE,
                null,
                executors
        ).register(sourceModule()).register(targetModule());
        return new DynamicRecordService(runtime, policyService);
    }

    private DynamicRecordService recordServiceWithReference(RecordGenerationRule rule) {
        DynamicActionExecutorRegistry executors = new DynamicActionExecutorRegistry(
                List.of(new RecordGenerationActionExecutor(ruleService)));
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(
                operations,
                new DynamicModuleRegistry(),
                DynamicFieldValueValidator.NONE,
                null,
                executors
        ).register(sourceModule()).register(targetModuleWithReference(rule));
        return new DynamicRecordService(runtime, policyService);
    }

    private RecordGenerationRule generationRule() {
        RecordGenerationRule rule = new RecordGenerationRule();
        rule.setSourceModuleAlias("sales.opportunity");
        rule.setTargetModuleAlias("sales.contract");
        rule.setActionCode("generateContract");
        rule.setTitle("生成合同");
        rule.setEnabled(Boolean.TRUE);
        return rule;
    }

    private RecordGenerationObjectMapping objectMapping(RecordGenerationFieldMapping... fieldMappings) {
        RecordGenerationObjectMapping mapping = new RecordGenerationObjectMapping();
        mapping.setSourceObjectAlias("opportunity");
        mapping.setTargetObjectAlias("contract");
        mapping.setFieldMappings(List.of(fieldMappings));
        return mapping;
    }

    private RecordGenerationFieldMapping fieldMapping(String targetField, String sourceField) {
        RecordGenerationFieldMapping mapping = new RecordGenerationFieldMapping();
        mapping.setTargetField(targetField);
        mapping.setSourceField(sourceField);
        mapping.setMappingType(RecordGenerationFieldSourceType.DIRECT);
        return mapping;
    }

    private RecordGenerationFieldMapping constantMapping(String targetField, String value) {
        RecordGenerationFieldMapping mapping = new RecordGenerationFieldMapping();
        mapping.setTargetField(targetField);
        mapping.setConstantValue(value);
        mapping.setMappingType(RecordGenerationFieldSourceType.CONSTANT);
        return mapping;
    }

    private RecordGenerationFieldMapping formulaMapping(String targetField, String expression) {
        RecordGenerationFieldMapping mapping = new RecordGenerationFieldMapping();
        mapping.setTargetField(targetField);
        mapping.setFormulaExpr(expression);
        mapping.setMappingType(RecordGenerationFieldSourceType.FORMULA);
        return mapping;
    }

    private ModuleDefinition sourceModule() {
        return new ModuleDefinition(
                "sales.opportunity",
                "Opportunity",
                List.of(new EntityDefinition("opportunity", "sales_opportunity", "Opportunity", List.of(
                        FieldDefinition.string("opportunityNo", "Opportunity No").column("opportunity_no"),
                        FieldDefinition.decimal("amount", "Amount").precision(18, 2)
                )).withCapabilities(EntityCapability.CRUD, EntityCapability.DATA_SCOPE),
                        new EntityDefinition("opportunity_line", "sales_opportunity_line", "Opportunity Line", List.of(
                                FieldDefinition.string("opportunityId", "Opportunity Id").column("opportunity_id"),
                                FieldDefinition.string("productName", "Product Name").column("product_name"),
                                FieldDefinition.decimal("lineAmount", "Line Amount").column("line_amount").precision(18, 2)
                        ))),
                List.of(EntityRelationDefinition.child("lines", "opportunity", "opportunity_line", "opportunityId")
                        .withAutoPopulate()),
                List.of(),
                List.of(),
                List.of(new EntityActionDefinition(
                        "opportunity",
                        "generateContract",
                        "生成合同",
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

    private ModuleDefinition targetModule() {
        return new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(new EntityDefinition("contract", "sales_contract", "Contract", List.of(
                        FieldDefinition.string("opportunityId", "Opportunity").column("opportunity_id"),
                        FieldDefinition.string("contractNo", "Contract No").column("contract_no"),
                        FieldDefinition.string("status", "Status"),
                        FieldDefinition.decimal("amountWithTax", "Amount With Tax").column("amount_with_tax").precision(18, 2)
                )),
                        new EntityDefinition("contract_line", "sales_contract_line", "Contract Line", List.of(
                                FieldDefinition.string("contractId", "Contract Id").column("contract_id"),
                                FieldDefinition.string("itemName", "Item Name").column("item_name"),
                                FieldDefinition.decimal("lineAmount", "Line Amount").column("line_amount").precision(18, 2)
                        ))),
                List.of(EntityRelationDefinition.child("lines", "contract", "contract_line", "contractId"))
        );
    }

    private ModuleDefinition targetModuleWithReference(RecordGenerationRule rule) {
        return new ModuleDefinition(
                "sales.contract",
                "Contract",
                targetModule().entities(),
                targetModule().relations(),
                List.of(EntityReferenceDefinition.to("contract", "opportunityId",
                                ReferenceTarget.of("sales.opportunity", "opportunity"))
                        .withRuntimeConfig(null, null, rule.getId(), null, Set.of()))
        );
    }

    private Map<String, Object> sourceRow() {
        return Map.of(
                "id", "opp-1",
                "opportunity_no", "OPP-001",
                "amount", new BigDecimal("100.00")
        );
    }

    private List<Map<String, Object>> sourceLineRows() {
        return List.of(
                Map.of("id", "line-1", "opportunity_id", "opp-1",
                        "product_name", "服务A", "line_amount", new BigDecimal("60.00")),
                Map.of("id", "line-2", "opportunity_id", "opp-1",
                        "product_name", "服务B", "line_amount", new BigDecimal("40.00"))
        );
    }

    private void stubSourceQueries() {
        when(operations.query(anyString(), anyMap())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("sales_opportunity_line")) {
                return sourceLineRows();
            }
            return List.of(sourceRow());
        });
    }

    @SuppressWarnings("unchecked")
    private IDatabaseOperations<Object> operations() {
        IDatabaseOperations<Object> operations = mock(IDatabaseOperations.class);
        when(operations.getDBInfo()).thenReturn(new DBInfo("POSTGRESQL").setName("muyun_test"));
        return operations;
    }

    private static final class RecordingPolicyService implements ActionExecutionPolicyService {
        private final List<String> actions = new ArrayList<>();

        @Override
        public void requireAuthorized(ActionExecutionContext context) {
            actions.add(context.moduleAlias() + ":" + context.actionCode() + ":" + context.recordIds());
        }

        List<String> actions() {
            return actions;
        }
    }
}
