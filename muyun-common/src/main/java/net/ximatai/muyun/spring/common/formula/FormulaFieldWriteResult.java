package net.ximatai.muyun.spring.common.formula;

public record FormulaFieldWriteResult(
        FormulaFieldPath fieldPath,
        boolean changed
) {
}
