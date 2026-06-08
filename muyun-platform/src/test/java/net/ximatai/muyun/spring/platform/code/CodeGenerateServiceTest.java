package net.ximatai.muyun.spring.platform.code;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.formula.FormulaEngine;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodeGenerateServiceTest {
    private final CodeRuleSegmentService segmentService = new CodeRuleSegmentService(new TestMemoryDao<>());
    private final CodeSequencePolicyService sequencePolicyService = new CodeSequencePolicyService(new TestMemoryDao<>());
    private final CodeValueMappingService mappingService = new CodeValueMappingService(new TestMemoryDao<>());
    private final CodeRuleService ruleService = new CodeRuleService(
            new TestMemoryDao<>(),
            segmentService,
            sequencePolicyService,
            mappingService
    );
    private final CodeSequenceStateService stateService = new CodeSequenceStateService(new TestMemoryDao<>());
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-08T00:00:00Z"), ZoneOffset.UTC);
    private final CodePreviewService previewService = new CodePreviewService(new FormulaEngine(clock), clock);
    private final CodeGenerateService generateService = new CodeGenerateService(ruleService, previewService, stateService, clock);

    @Test
    void shouldAdvanceFormalSequenceButPreviewDoesNotAdvance() {
        CodeRule rule = rule("orderNo", CodeMode.AUTO, CodeSequenceResetPolicy.NONE);
        ruleService.saveRuleTree(rule);

        assertThat(previewService.previewDraft(new PreviewCodeRuleCommand(ruleService.viewRuleTree(rule.getId()),
                Map.of(), null, at("2026-06-08T10:00:00"), null)).value())
                .isEqualTo("SO-0001");
        assertThat(generate("orderNo", Map.of(), at("2026-06-08T10:00:00")).value()).isEqualTo("SO-0001");
        assertThat(previewService.previewDraft(new PreviewCodeRuleCommand(ruleService.viewRuleTree(rule.getId()),
                Map.of(), null, at("2026-06-08T10:00:00"), null)).value())
                .isEqualTo("SO-0001");
        assertThat(generate("orderNo", Map.of(), at("2026-06-08T10:00:00")).value()).isEqualTo("SO-0002");
    }

    @Test
    void shouldUseIndependentSequenceBucketsForDifferentBasisKeys() {
        CodeRule rule = rule("orderNo", CodeMode.AUTO, CodeSequenceResetPolicy.NONE);
        CodeRuleSegment basis = segment(CodeSegmentType.FIELD_VALUE, null, "orderType");
        basis.setSequenceBasis(Boolean.TRUE);
        rule.getSegments().add(1, basis);
        ruleService.saveRuleTree(rule);

        assertThat(generate("orderNo", Map.of("orderType", "A"), at("2026-06-08T10:00:00")).value())
                .isEqualTo("SO-A0001");
        assertThat(generate("orderNo", Map.of("orderType", "B"), at("2026-06-08T10:00:00")).value())
                .isEqualTo("SO-B0001");
        assertThat(generate("orderNo", Map.of("orderType", "A"), at("2026-06-08T10:00:00")).value())
                .isEqualTo("SO-A0002");
    }

    @Test
    void shouldUseIndependentSequenceBucketsForDifferentPeriodKeys() {
        CodeRule rule = rule("orderNo", CodeMode.AUTO, CodeSequenceResetPolicy.MONTH);
        ruleService.saveRuleTree(rule);

        assertThat(generate("orderNo", Map.of(), at("2026-06-08T10:00:00")).value()).isEqualTo("SO-0001");
        assertThat(generate("orderNo", Map.of(), at("2026-07-01T10:00:00")).value()).isEqualTo("SO-0001");
        assertThat(generate("orderNo", Map.of(), at("2026-06-09T10:00:00")).value()).isEqualTo("SO-0002");
    }

    @Test
    void shouldRejectManualRuleAutomaticGeneration() {
        CodeRule rule = rule("orderNo", CodeMode.MANUAL, CodeSequenceResetPolicy.NONE);

        assertThatThrownBy(() -> ruleService.saveRuleTree(rule))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("MANUAL code rule cannot declare automatic sequence configuration");
    }

    @Test
    void shouldRejectDuplicateGeneratedValueWhenUniquenessCheckerMatches() {
        CodeRule rule = rule("orderNo", CodeMode.AUTO, CodeSequenceResetPolicy.NONE);
        ruleService.saveRuleTree(rule);

        assertThatThrownBy(() -> generateService.generate(new GenerateCodeCommand(
                "crm.order",
                "main",
                null,
                "orderNo",
                null,
                at("2026-06-08T10:00:00"),
                Map.of(),
                (resolved, value, context) -> true
        )))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldResetSequenceWhenOverflowPolicyAllowsReset() {
        CodeRule rule = rule("orderNo", CodeMode.AUTO, CodeSequenceResetPolicy.NONE);
        rule.getSequencePolicy().setMaxValue(2L);
        rule.getSequencePolicy().setOverflowPolicy(CodeSequenceOverflowPolicy.RESET);
        ruleService.saveRuleTree(rule);

        assertThat(generate("orderNo", Map.of(), at("2026-06-08T10:00:00")).value()).isEqualTo("SO-0001");
        assertThat(generate("orderNo", Map.of(), at("2026-06-08T10:00:00")).value()).isEqualTo("SO-0002");
        assertThat(generate("orderNo", Map.of(), at("2026-06-08T10:00:00")).value()).isEqualTo("SO-0001");
    }

    @Test
    void shouldResetSequenceByLengthOverflowWhenPolicyAllowsReset() {
        CodeRule rule = rule("orderNo", CodeMode.AUTO, CodeSequenceResetPolicy.NONE);
        rule.getSequencePolicy().setStartValue(9L);
        rule.getSequencePolicy().setSequenceLength(1);
        rule.getSequencePolicy().setOverflowPolicy(CodeSequenceOverflowPolicy.RESET);
        rule.getSegments().stream()
                .filter(segment -> segment.getSegmentType() == CodeSegmentType.SEQUENCE)
                .findFirst()
                .orElseThrow()
                .setLength(1);
        ruleService.saveRuleTree(rule);

        assertThat(generate("orderNo", Map.of(), at("2026-06-08T10:00:00")).value()).isEqualTo("SO-9");
        assertThat(generate("orderNo", Map.of(), at("2026-06-08T10:00:00")).value()).isEqualTo("SO-9");
    }

    @Test
    void shouldRejectSequenceOverflowByDefault() {
        CodeRule rule = rule("orderNo", CodeMode.AUTO, CodeSequenceResetPolicy.NONE);
        rule.getSequencePolicy().setMaxValue(1L);
        ruleService.saveRuleTree(rule);

        assertThat(generate("orderNo", Map.of(), at("2026-06-08T10:00:00")).value()).isEqualTo("SO-0001");
        assertThatThrownBy(() -> generate("orderNo", Map.of(), at("2026-06-08T10:00:00")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("overflow");
    }

    private GenerateCodeResult generate(String fieldName, Map<String, Object> context, LocalDateTime at) {
        return generateService.generate(new GenerateCodeCommand(
                "crm.order",
                "main",
                null,
                fieldName,
                null,
                at,
                context,
                null
        ));
    }

    private CodeRule rule(String fieldName, CodeMode mode, CodeSequenceResetPolicy resetPolicy) {
        CodeRule rule = new CodeRule();
        rule.setModuleAlias("crm.order");
        rule.setEntityAlias("main");
        rule.setFieldName(fieldName);
        rule.setFieldRole(CodeFieldRole.PRIMARY);
        rule.setMode(mode);
        rule.setEnabled(Boolean.TRUE);
        rule.setSegments(new java.util.ArrayList<>(java.util.List.of(
                segment(CodeSegmentType.CONSTANT, "SO-", null),
                sequenceSegment()
        )));
        CodeSequencePolicy policy = new CodeSequencePolicy();
        policy.setStartValue(1L);
        policy.setStepValue(1L);
        policy.setSequenceLength(4);
        policy.setResetPolicy(resetPolicy);
        rule.setSequencePolicy(policy);
        return rule;
    }

    private CodeRuleSegment sequenceSegment() {
        CodeRuleSegment segment = segment(CodeSegmentType.SEQUENCE, null, null);
        segment.setLength(4);
        return segment;
    }

    private CodeRuleSegment segment(CodeSegmentType type, String fixedValue, String sourceRef) {
        CodeRuleSegment segment = new CodeRuleSegment();
        segment.setSegmentType(type);
        segment.setFixedValue(fixedValue);
        segment.setSourceRef(sourceRef);
        return segment;
    }

    private LocalDateTime at(String value) {
        return LocalDateTime.parse(value);
    }
}
