package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.common.formula.FormulaIssueLevel;
import net.ximatai.muyun.spring.common.formula.FormulaRuleKind;
import net.ximatai.muyun.spring.common.formula.FormulaRulePhase;
import net.ximatai.muyun.spring.dynamic.metadata.EntityFormulaRuleDefinition;

public record DynamicFormulaRuleDescriptor(
        String code,
        String expression,
        FormulaRuleKind kind,
        FormulaRulePhase phase,
        String targetField,
        FormulaIssueLevel severity,
        String messageTemplate,
        boolean stopOnError,
        boolean enabled,
        int sortOrder
) {
    public static DynamicFormulaRuleDescriptor from(EntityFormulaRuleDefinition rule) {
        return new DynamicFormulaRuleDescriptor(
                rule.code(),
                rule.expression(),
                rule.kind(),
                rule.phase(),
                rule.targetField(),
                rule.severity(),
                rule.messageTemplate(),
                rule.stopOnError(),
                rule.enabled(),
                rule.sortOrder()
        );
    }
}
