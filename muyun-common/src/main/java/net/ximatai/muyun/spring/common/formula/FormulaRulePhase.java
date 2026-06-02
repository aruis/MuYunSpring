package net.ximatai.muyun.spring.common.formula;

public enum FormulaRulePhase {
    DEFAULT_VALUE,
    BEFORE_SAVE,
    ACTION_AVAILABLE,
    ACTION_BEFORE_EXECUTE,
    WORKFLOW_CONDITION,
    IMPORT_VALIDATE,
    CUSTOM
}
