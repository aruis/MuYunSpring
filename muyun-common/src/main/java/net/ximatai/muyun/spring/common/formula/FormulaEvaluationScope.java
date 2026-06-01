package net.ximatai.muyun.spring.common.formula;

public record FormulaEvaluationScope(String tableKey, Object row) {
    public static FormulaEvaluationScope main() {
        return new FormulaEvaluationScope(null, null);
    }

    public static FormulaEvaluationScope row(String tableKey, Object row) {
        return new FormulaEvaluationScope(tableKey, row);
    }
}
