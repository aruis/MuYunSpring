package net.ximatai.muyun.spring.common.formula;

public class FormulaEvaluationException extends RuntimeException {
    private final String code;
    private final String fieldPath;
    private final Integer position;

    public FormulaEvaluationException(String code, String message) {
        this(code, null, null, message);
    }

    public FormulaEvaluationException(String code, String fieldPath, String message) {
        this(code, fieldPath, null, message);
    }

    public FormulaEvaluationException(String code, String fieldPath, Integer position, String message) {
        super(message);
        this.code = code == null || code.isBlank() ? "FORMULA_EVALUATE_ERROR" : code;
        this.fieldPath = fieldPath;
        this.position = position;
    }

    public String code() {
        return code;
    }

    public String fieldPath() {
        return fieldPath;
    }

    public Integer position() {
        return position;
    }
}
