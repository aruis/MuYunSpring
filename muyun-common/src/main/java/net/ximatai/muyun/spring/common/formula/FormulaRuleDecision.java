package net.ximatai.muyun.spring.common.formula;

public record FormulaRuleDecision(
        String ruleId,
        FormulaRuleKind kind,
        FormulaRulePhase phase,
        String targetField,
        boolean matched
) {
}
