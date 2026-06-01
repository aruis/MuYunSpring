package net.ximatai.muyun.spring.common.formula;

import java.util.List;

public interface FormulaEvaluationContext {
    Object get(FormulaFieldPath fieldPath, FormulaEvaluationScope scope);

    FormulaFieldWriteResult set(FormulaFieldPath fieldPath, Object value, FormulaEvaluationScope scope);

    List<?> rows(String tableKey);
}
