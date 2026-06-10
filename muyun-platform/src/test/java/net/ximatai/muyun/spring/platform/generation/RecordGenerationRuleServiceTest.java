package net.ximatai.muyun.spring.platform.generation;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.module.ModuleActionContributionRegistrar;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordGenerationRuleServiceTest {
    private final RecordGenerationObjectMappingService objectMappingService = new RecordGenerationObjectMappingService(new TestMemoryDao<>());
    private final RecordGenerationFieldMappingService fieldMappingService = new RecordGenerationFieldMappingService(new TestMemoryDao<>());
    private final RecordGenerationSplitPolicyService splitPolicyService = new RecordGenerationSplitPolicyService(new TestMemoryDao<>());
    private final RecordGenerationSplitGroupFieldService splitGroupFieldService = new RecordGenerationSplitGroupFieldService(new TestMemoryDao<>());

    @Test
    void shouldSaveAndViewGenerationRuleTree() {
        RecordGenerationRuleService ruleService = ruleServiceWithoutContributor();
        RecordGenerationRule rule = baseRule();
        RecordGenerationObjectMapping mapping = objectMapping(
                fieldMapping("contractNo", "sourceNo"),
                constantMapping("status", "draft"),
                formulaMapping("amountWithTax", "amount * 1.06"));
        mapping.setSplitDriver(Boolean.TRUE);
        mapping.setSplitPolicy(groupSplit("productId"));
        rule.setObjectMappings(List.of(mapping));

        RecordGenerationRule saved = ruleService.saveRuleTree(rule);

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getActionCode()).isEqualTo("generateInvoice");
        assertThat(saved.getObjectMappings()).singleElement()
                .satisfies(objectMapping -> {
                    assertThat(objectMapping.getTargetObjectAlias()).isEqualTo("main");
                    assertThat(objectMapping.getFieldMappings()).extracting(RecordGenerationFieldMapping::getTargetField)
                            .containsExactly("contractNo", "status", "amountWithTax");
                    assertThat(objectMapping.getFieldMappings()).extracting(RecordGenerationFieldMapping::getMappingType)
                            .containsExactly(RecordGenerationFieldSourceType.DIRECT, RecordGenerationFieldSourceType.CONSTANT, RecordGenerationFieldSourceType.FORMULA);
                    assertThat(objectMapping.getFieldMappings().getFirst().getDefaultValue()).isEqualTo("N/A");
                    assertThat(objectMapping.getSplitPolicy().getGroupFields()).singleElement()
                            .extracting(RecordGenerationSplitGroupField::getFieldName)
                            .isEqualTo("productId");
                });
    }

    @Test
    void shouldSyncActionWhenEnabledAndDisableWhenRuleDisabled() {
        PlatformModuleService moduleService = new PlatformModuleService(new TestMemoryDao<>());
        PlatformModuleActionService actionService =
                new PlatformModuleActionService(new TestMemoryDao<>(), moduleService);
        moduleService.insert(module("sales.contract"));
        GenerationModuleActionContributor contributor =
                new GenerationModuleActionContributor(new ModuleActionContributionRegistrar(actionService));
        RecordGenerationRuleService ruleService = ruleService(Optional.of(contributor));

        RecordGenerationRule saved = ruleService.saveRuleTree(baseRule());
        PlatformModuleAction action = actionService.findByModuleAliasAndActionCode("sales.contract", "generateInvoice");
        assertThat(action.getEnabled()).isTrue();

        saved.setEnabled(Boolean.FALSE);
        ruleService.saveRuleTree(saved);

        assertThat(actionService.findByModuleAliasAndActionCode("sales.contract", "generateInvoice").getEnabled())
                .isFalse();
    }

    @Test
    void shouldRejectBlankSourceOrTargetModuleAlias() {
        RecordGenerationRuleService ruleService = ruleServiceWithoutContributor();
        RecordGenerationRule blankSource = baseRule();
        blankSource.setSourceModuleAlias(" ");
        assertThatThrownBy(() -> ruleService.saveRuleTree(blankSource))
                .isInstanceOf(RuntimeException.class);

        RecordGenerationRule blankTarget = baseRule();
        blankTarget.setTargetModuleAlias(null);
        assertThatThrownBy(() -> ruleService.saveRuleTree(blankTarget))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldRejectRecordGenerationObjectMappingWithoutRuleId() {
        RecordGenerationObjectMapping objectMapping = new RecordGenerationObjectMapping();
        objectMapping.setTargetObjectAlias("main");

        assertThatThrownBy(() -> objectMappingService.insert(objectMapping))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("requires ruleId");
    }

    @Test
    void shouldRejectDuplicateTargetFieldInOneRecordGenerationObjectMapping() {
        RecordGenerationRuleService ruleService = ruleServiceWithoutContributor();
        RecordGenerationRule rule = baseRule();
        rule.setObjectMappings(List.of(objectMapping(
                fieldMapping("contractNo", "sourceNo"),
                fieldMapping("contractNo", "externalNo"))));

        assertThatThrownBy(() -> ruleService.saveRuleTree(rule))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("duplicate targetField");
    }

    @Test
    void shouldRejectDuplicateActionCodeWithinSourceModule() {
        RecordGenerationRuleService ruleService = ruleServiceWithoutContributor();
        ruleService.saveRuleTree(baseRule());
        RecordGenerationRule duplicate = baseRule();

        assertThatThrownBy(() -> ruleService.saveRuleTree(duplicate))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("actionCode must be unique");
    }

    @Test
    void shouldRejectMultipleSplitDriverObjectMappings() {
        RecordGenerationRuleService ruleService = ruleServiceWithoutContributor();
        RecordGenerationRule rule = baseRule();
        RecordGenerationObjectMapping first = objectMapping(fieldMapping("contractNo", "sourceNo"));
        first.setSplitDriver(Boolean.TRUE);
        first.setSplitPolicy(groupSplit("productId"));
        RecordGenerationObjectMapping second = objectMapping(fieldMapping("lineNo", "sourceLineNo"));
        second.setSplitDriver(Boolean.TRUE);
        second.setSplitPolicy(groupSplit("warehouseId"));
        rule.setObjectMappings(List.of(first, second));

        assertThatThrownBy(() -> ruleService.saveRuleTree(rule))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("one split driver object mapping");
    }

    @Test
    void shouldAllowGroupAndQuantitySplitOnSingleDriverObjectMapping() {
        RecordGenerationRuleService ruleService = ruleServiceWithoutContributor();
        RecordGenerationRule rule = baseRule();
        RecordGenerationObjectMapping mapping = objectMapping(fieldMapping("contractNo", "sourceNo"));
        RecordGenerationSplitPolicy splitPolicy = groupSplit("productId");
        splitPolicy.setQuantityField("quantity");
        splitPolicy.setQuantityStep(1);
        mapping.setSplitDriver(Boolean.TRUE);
        mapping.setSplitPolicy(splitPolicy);
        rule.setObjectMappings(List.of(mapping));

        RecordGenerationRule saved = ruleService.saveRuleTree(rule);

        assertThat(saved.getObjectMappings()).singleElement()
                .satisfies(savedMapping -> {
                    assertThat(savedMapping.getSplitDriver()).isTrue();
                    assertThat(savedMapping.getSplitPolicy().getQuantityField()).isEqualTo("quantity");
                    assertThat(savedMapping.getSplitPolicy().getGroupFields()).singleElement()
                            .extracting(RecordGenerationSplitGroupField::getFieldName)
                            .isEqualTo("productId");
                });
    }

    @Test
    void shouldRejectSplitPolicyOnNonDriverObjectMapping() {
        RecordGenerationRuleService ruleService = ruleServiceWithoutContributor();
        RecordGenerationRule rule = baseRule();
        RecordGenerationObjectMapping mapping = objectMapping(fieldMapping("contractNo", "sourceNo"));
        mapping.setSplitPolicy(groupSplit("productId"));
        rule.setObjectMappings(List.of(mapping));

        assertThatThrownBy(() -> ruleService.saveRuleTree(rule))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("requires splitDriver");
    }

    private RecordGenerationRuleService ruleServiceWithoutContributor() {
        return ruleService(Optional.empty());
    }

    private RecordGenerationRuleService ruleService(Optional<GenerationModuleActionContributor> contributor) {
        return new RecordGenerationRuleService(
                new TestMemoryDao<>(),
                objectMappingService,
                fieldMappingService,
                splitPolicyService,
                splitGroupFieldService,
                contributor);
    }

    private RecordGenerationRule baseRule() {
        RecordGenerationRule rule = new RecordGenerationRule();
        rule.setSourceModuleAlias("sales.contract");
        rule.setTargetModuleAlias("finance.invoice");
        rule.setActionCode("generateInvoice");
        rule.setTitle("生成发票");
        rule.setEnabled(Boolean.TRUE);
        return rule;
    }

    private RecordGenerationObjectMapping objectMapping(RecordGenerationFieldMapping... fieldMappings) {
        RecordGenerationObjectMapping mapping = new RecordGenerationObjectMapping();
        mapping.setSourceObjectAlias("main");
        mapping.setTargetObjectAlias("main");
        mapping.setFieldMappings(List.of(fieldMappings));
        return mapping;
    }

    private RecordGenerationFieldMapping fieldMapping(String targetField, String sourceField) {
        RecordGenerationFieldMapping mapping = new RecordGenerationFieldMapping();
        mapping.setTargetField(targetField);
        mapping.setSourceField(sourceField);
        mapping.setMappingType(RecordGenerationFieldSourceType.DIRECT);
        mapping.setDefaultValue("N/A");
        return mapping;
    }

    private RecordGenerationFieldMapping constantMapping(String targetField, String constantValue) {
        RecordGenerationFieldMapping mapping = new RecordGenerationFieldMapping();
        mapping.setTargetField(targetField);
        mapping.setMappingType(RecordGenerationFieldSourceType.CONSTANT);
        mapping.setConstantValue(constantValue);
        return mapping;
    }

    private RecordGenerationFieldMapping formulaMapping(String targetField, String formulaExpr) {
        RecordGenerationFieldMapping mapping = new RecordGenerationFieldMapping();
        mapping.setTargetField(targetField);
        mapping.setMappingType(RecordGenerationFieldSourceType.FORMULA);
        mapping.setFormulaExpr(formulaExpr);
        return mapping;
    }

    private RecordGenerationSplitPolicy groupSplit(String fieldName) {
        RecordGenerationSplitPolicy policy = new RecordGenerationSplitPolicy();
        RecordGenerationSplitGroupField groupField = new RecordGenerationSplitGroupField();
        groupField.setFieldName(fieldName);
        policy.setGroupFields(List.of(groupField));
        return policy;
    }

    private PlatformModule module(String alias) {
        PlatformModule module = new PlatformModule();
        module.setApplicationAlias(alias.substring(0, alias.indexOf('.')));
        module.setAlias(alias);
        module.setTitle(alias);
        return module;
    }
}
