package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.formula.FormulaIssueLevel;
import net.ximatai.muyun.spring.common.formula.FormulaRule;
import net.ximatai.muyun.spring.common.formula.FormulaRuleKind;
import net.ximatai.muyun.spring.common.formula.FormulaRulePhase;

public record EntityFormulaRuleDefinition(
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
    public EntityFormulaRuleDefinition(String code, String expression) {
        this(code, expression, FormulaRuleKind.CALCULATION, FormulaRulePhase.BEFORE_SAVE, null);
    }

    public EntityFormulaRuleDefinition(String code,
                                       String expression,
                                       FormulaRuleKind kind,
                                       FormulaRulePhase phase,
                                       String targetField) {
        this(code, expression, kind, phase, targetField, FormulaIssueLevel.ERROR, null, false, true, 0);
    }

    public EntityFormulaRuleDefinition {
        code = code == null ? null : code.trim();
        expression = expression == null ? null : expression.trim();
        kind = kind == null ? FormulaRuleKind.CALCULATION : kind;
        phase = phase == null ? FormulaRulePhase.BEFORE_SAVE : phase;
        targetField = targetField == null || targetField.isBlank() ? null : targetField.trim();
        severity = severity == null ? FormulaIssueLevel.ERROR : severity;
        messageTemplate = messageTemplate == null || messageTemplate.isBlank() ? null : messageTemplate.trim();
    }

    public static EntityFormulaRuleDefinition calculation(String code, String expression) {
        return new EntityFormulaRuleDefinition(code, expression);
    }

    public static EntityFormulaRuleDefinition calculation(String code, String targetField, String expression) {
        return new EntityFormulaRuleDefinition(code, expression,
                FormulaRuleKind.CALCULATION, FormulaRulePhase.BEFORE_SAVE, targetField);
    }

    public static EntityFormulaRuleDefinition validation(String code,
                                                         String targetField,
                                                         String expression,
                                                         String messageTemplate) {
        return new EntityFormulaRuleDefinition(code, expression,
                FormulaRuleKind.VALIDATION, FormulaRulePhase.BEFORE_SAVE, targetField,
                FormulaIssueLevel.ERROR, messageTemplate, true, true, 0);
    }

    public EntityFormulaRuleDefinition phase(FormulaRulePhase value) {
        return new EntityFormulaRuleDefinition(code, expression, kind, value, targetField,
                severity, messageTemplate, stopOnError, enabled, sortOrder);
    }

    public EntityFormulaRuleDefinition severity(FormulaIssueLevel value) {
        return new EntityFormulaRuleDefinition(code, expression, kind, phase, targetField,
                value, messageTemplate, stopOnError, enabled, sortOrder);
    }

    public EntityFormulaRuleDefinition message(String value) {
        return new EntityFormulaRuleDefinition(code, expression, kind, phase, targetField,
                severity, value, stopOnError, enabled, sortOrder);
    }

    public EntityFormulaRuleDefinition stoppingOnError() {
        return new EntityFormulaRuleDefinition(code, expression, kind, phase, targetField,
                severity, messageTemplate, true, enabled, sortOrder);
    }

    public EntityFormulaRuleDefinition disabled() {
        return new EntityFormulaRuleDefinition(code, expression, kind, phase, targetField,
                severity, messageTemplate, stopOnError, false, sortOrder);
    }

    public EntityFormulaRuleDefinition sortOrder(int value) {
        return new EntityFormulaRuleDefinition(code, expression, kind, phase, targetField,
                severity, messageTemplate, stopOnError, enabled, value);
    }

    public FormulaRule toRuntimeRule() {
        return new FormulaRule(code, expression, kind, phase, targetField,
                severity, messageTemplate, stopOnError, enabled);
    }
}
