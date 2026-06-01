package net.ximatai.muyun.spring.common.formula;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FormulaEngineTest {
    private final FormulaEngine engine = new FormulaEngine(
            Clock.fixed(Instant.parse("2026-06-01T02:03:04Z"), ZoneOffset.UTC)
    );

    @Test
    void shouldEvaluateArithmeticComparisonAndFunctions() {
        FormulaRuntimeData data = FormulaRuntimeData.of(new LinkedHashMap<>(Map.of(
                "amount", 120,
                "discount", 20,
                "name", "Contract"
        )));

        assertThat(engine.evaluateValue("({amount} - {discount}) * 2", data)).isEqualTo(200d);
        assertThat(engine.evaluateBoolean("{amount} >= 100 && {discount} == 20", data)).isTrue();
        assertThat(engine.evaluateValue("CONCAT(UPPER({name}), '-', ROUND(12.345, 2))", data))
                .isEqualTo("CONTRACT-12.35");
        assertThat(engine.evaluateValue("IF(ISNULL({missing}), 'empty', 'filled')", data)).isEqualTo("empty");
    }

    @Test
    void shouldApplyAssignmentsToMainRecordAndAggregateChildren() {
        Map<String, Object> main = new LinkedHashMap<>();
        main.put("amount", 100);
        main.put("discount", 15);
        FormulaRuntimeData data = FormulaRuntimeData.of(main, Map.of(
                "items", List.of(
                        new LinkedHashMap<>(Map.of("qty", 2, "price", 10)),
                        new LinkedHashMap<>(Map.of("qty", 3, "price", 20))
                )
        ));

        FormulaExecutionResult result = engine.execute(List.of(
                new FormulaRule("net", "{netAmount} = {amount} - {discount}"),
                new FormulaRule("totalQty", "SUM({items.qty})", FormulaRuleKind.CALCULATION, FormulaRulePhase.BEFORE_SAVE, "totalQty")
        ), data);

        assertThat(result.report().errors()).isEmpty();
        assertThat(result.changedFields()).containsExactly("netAmount", "totalQty");
        assertThat(main).containsEntry("netAmount", 85d);
        assertThat(main).containsEntry("totalQty", 5d);
    }

    @Test
    void shouldSupportDateAndDateTimeFunctions() {
        FormulaRuntimeData data = FormulaRuntimeData.of(new LinkedHashMap<>(Map.of(
                "signedDate", "2026-06-01",
                "signedAt", "2026-06-01T02:03:04Z"
        )));

        assertThat(engine.evaluateValue("TODAY()", data)).isEqualTo("2026-06-01");
        assertThat(engine.evaluateValue("NOW()", data)).isEqualTo("2026-06-01T02:03:04Z");
        assertThat(engine.evaluateValue("DATE_ADD({signedDate}, 3)", data)).isEqualTo("2026-06-04");
        assertThat(engine.evaluateValue("DATETIME_ADD({signedAt}, 2, 'HOUR')", data)).isEqualTo("2026-06-01T04:03:04Z");
        assertThat(engine.evaluateValue("DATE_DIFF_HOURS('2026-06-01T05:03:04Z', {signedAt})", data)).isEqualTo(3d);
    }

    @Test
    void shouldReportFormulaErrorsAndWarnings() {
        Map<String, Object> main = new LinkedHashMap<>(Map.of("amount", 100));
        FormulaRuntimeData data = FormulaRuntimeData.of(main);

        FormulaRuntimeReport report = engine.apply(List.of(
                new FormulaRule("missingTarget", "{amount} + 1"),
                new FormulaRule("badDate", "{date} = DATE_ADD('bad', 1)")
        ), data);

        assertThat(report.warnings()).isEmpty();
        assertThat(report.errors()).extracting(FormulaRuntimeReport.Issue::ruleId)
                .containsExactly("missingTarget", "badDate");
        assertThat(report.errors()).first()
                .extracting(FormulaRuntimeReport.Issue::code)
                .isEqualTo("FORMULA_ASSIGNMENT_REQUIRED");
    }

    @Test
    void shouldSupportValidationAndWarningRules() {
        FormulaRuntimeData data = FormulaRuntimeData.of(new LinkedHashMap<>(Map.of("amount", 80)));

        FormulaExecutionResult result = engine.execute(List.of(
                new FormulaRule("amountRequired", "{amount} >= 100", FormulaRuleKind.VALIDATION,
                        FormulaRulePhase.BEFORE_SAVE, "amount", FormulaIssueLevel.WARNING, "amount is low", false, true)
        ), data);

        FormulaRuntimeReport report = result.report();
        assertThat(report.errors()).isEmpty();
        assertThat(result.decisions()).singleElement()
                .extracting(FormulaRuleDecision::matched)
                .isEqualTo(false);
        assertThat(report.warnings()).singleElement()
                .extracting(FormulaRuntimeReport.Issue::ruleId)
                .isEqualTo("amountRequired");
        assertThat(report.warnings()).singleElement()
                .extracting(FormulaRuntimeReport.Issue::fieldPath)
                .isEqualTo("amount");
    }

    @Test
    void shouldShortCircuitLazyBranches() {
        FormulaRuntimeData data = FormulaRuntimeData.of(new LinkedHashMap<>(Map.of("enabled", false)));

        assertThat(engine.evaluateValue("IF({enabled}, DATE_ADD('bad', 1), 'skip')", data)).isEqualTo("skip");
        assertThat(engine.evaluateBoolean("false && DATE_ADD('bad', 1)", data)).isFalse();
        assertThat(engine.evaluateBoolean("true || DATE_ADD('bad', 1)", data)).isTrue();
    }

    @Test
    void shouldReportUnknownFunctionAndStrictUnknownField() {
        FormulaRuntimeData strictData = FormulaRuntimeData.strict(
                new LinkedHashMap<>(Map.of("amount", 100)),
                Map.of(),
                Set.of("amount", "date")
        );

        FormulaRuntimeReport report = engine.apply(List.of(
                new FormulaRule("unknownFunction", "{date} = UNKNOWN_FN({amount})"),
                new FormulaRule("unknownField", "{date} = {missing} + 1")
        ), strictData);

        assertThat(report.errors()).extracting(FormulaRuntimeReport.Issue::ruleId)
                .containsExactly("unknownFunction", "unknownField");
        assertThat(report.errors()).extracting(FormulaRuntimeReport.Issue::code)
                .containsExactly("FORMULA_UNKNOWN_FUNCTION", "FORMULA_UNKNOWN_FIELD");
        assertThat(report.errors()).extracting(FormulaRuntimeReport.Issue::fieldPath)
                .containsExactly(null, "missing");
        assertThat(report.errors()).extracting(FormulaRuntimeReport.Issue::message)
                .anyMatch(message -> message.contains("unknown formula function"))
                .anyMatch(message -> message.contains("unknown formula field"));
    }

    @Test
    void shouldRejectAggregateAcrossMultipleChildTables() {
        FormulaRuntimeData data = FormulaRuntimeData.of(new LinkedHashMap<>(), Map.of(
                "items", List.of(new LinkedHashMap<>(Map.of("amount", 10))),
                "payments", List.of(new LinkedHashMap<>(Map.of("amount", 5)))
        ));

        FormulaRuntimeReport report = engine.apply(List.of(
                new FormulaRule("mixedAggregate", "{total} = SUM({items.amount}, {payments.amount})")
        ), data);

        assertThat(report.errors()).singleElement()
                .extracting(FormulaRuntimeReport.Issue::code)
                .isEqualTo("FORMULA_AGGREGATE_SCOPE_ERROR");
    }

    @Test
    void shouldStopOnReportedErrorAndRejectNestedAssignment() {
        Map<String, Object> main = new LinkedHashMap<>();
        FormulaRuntimeData data = FormulaRuntimeData.of(main);

        FormulaRuntimeReport report = engine.apply(List.of(
                new FormulaRule("nested", "({a} = 1) + UNKNOWN_FN()", FormulaRuleKind.CALCULATION,
                        FormulaRulePhase.BEFORE_SAVE, null, FormulaIssueLevel.ERROR, null, true, true),
                new FormulaRule("next", "{b} = 2")
        ), data);

        assertThat(report.errors()).singleElement()
                .extracting(FormulaRuntimeReport.Issue::code)
                .isEqualTo("FORMULA_ASSIGNMENT_SCOPE_ERROR");
        assertThat(main).doesNotContainKeys("a", "b");
    }

    @Test
    void shouldReadExpressionFromJsonLikePayload() {
        FormulaRuntimeData data = FormulaRuntimeData.of(new LinkedHashMap<>(Map.of("amount", 12)));

        assertThat(engine.evaluateValue("{\"expression\":\"{amount} * 2\"}", data)).isEqualTo(24d);
    }
}
