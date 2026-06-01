package net.ximatai.muyun.spring.common.formula;

import java.util.Objects;

public record FormulaRule(
        String id,
        String expression,
        FormulaRuleKind kind,
        FormulaRulePhase phase,
        String targetField,
        FormulaIssueLevel severity,
        String messageTemplate,
        boolean stopOnError,
        boolean enabled
) {
    public FormulaRule(String id, String expression) {
        this(id, expression, FormulaRuleKind.CALCULATION, FormulaRulePhase.BEFORE_SAVE, null);
    }

    public FormulaRule(String id, String expression, boolean enabled) {
        this(id, expression, FormulaRuleKind.CALCULATION, FormulaRulePhase.BEFORE_SAVE, null,
                FormulaIssueLevel.ERROR, null, false, enabled);
    }

    public FormulaRule(String id, String expression, FormulaRuleKind kind, FormulaRulePhase phase, String targetField) {
        this(id, expression, kind, phase, targetField, FormulaIssueLevel.ERROR, null, false, true);
    }

    public FormulaRule(
            String id,
            String expression,
            FormulaRuleKind kind,
            FormulaRulePhase phase,
            String targetField,
            FormulaIssueLevel severity,
            String messageTemplate,
            boolean stopOnError,
            boolean enabled
    ) {
        id = id == null || id.isBlank() ? "formula" : id;
        expression = Objects.requireNonNullElse(expression, "").trim();
        kind = kind == null ? FormulaRuleKind.CALCULATION : kind;
        phase = phase == null ? FormulaRulePhase.BEFORE_SAVE : phase;
        targetField = targetField == null || targetField.isBlank() ? null : targetField.trim();
        severity = severity == null ? FormulaIssueLevel.ERROR : severity;
        messageTemplate = messageTemplate == null || messageTemplate.isBlank() ? null : messageTemplate.trim();
        this.id = id;
        this.expression = expression;
        this.kind = kind;
        this.phase = phase;
        this.targetField = targetField;
        this.severity = severity;
        this.messageTemplate = messageTemplate;
        this.stopOnError = stopOnError;
        this.enabled = enabled;
    }
}
