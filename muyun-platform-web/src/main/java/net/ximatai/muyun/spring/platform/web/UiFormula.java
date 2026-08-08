package net.ximatai.muyun.spring.platform.web;

import java.util.regex.Pattern;

/**
 * A portable Boolean predicate evaluated by platform Web clients against the current draft.
 *
 * <p>This is intentionally not an entry point to the server-side {@code FormulaEngine}: a UI descriptor is
 * evaluated in browsers too, so it may only use the small, explicitly portable grammar below.  Extending this
 * grammar is a cross-client platform change: the server validator and every Web evaluator must be extended in
 * the same release.</p>
 */
public record UiFormula(String expression) {
    private static final Pattern PRESENT_FIELD = Pattern.compile("PRESENT\\(\\{[A-Za-z][A-Za-z0-9_]*}\\)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NEGATED_PRESENT_FIELD = Pattern.compile("!\\(PRESENT\\(\\{[A-Za-z][A-Za-z0-9_]*}\\)\\)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PRESENT_TERM = Pattern.compile(
            "(?:PRESENT\\(\\{[A-Za-z][A-Za-z0-9_]*}\\)|!\\(PRESENT\\(\\{[A-Za-z][A-Za-z0-9_]*}\\)\\))",
            Pattern.CASE_INSENSITIVE);

    public UiFormula {
        if (expression == null || expression.isBlank()) throw new IllegalArgumentException("ui formula expression must not be blank");
        expression = expression.trim();
        if (!isPortableBooleanExpression(expression)) {
            throw new IllegalArgumentException("unsupported portable UI Boolean formula: " + expression
                    + "; supported expressions use PRESENT({fieldName}), negation, and && conjunction");
        }
    }

    /**
     * Creates a Boolean predicate from the portable UI formula grammar.
     */
    public static UiFormula booleanExpression(String expression) {
        return new UiFormula(expression);
    }

    UiFormula negated() {
        if (NEGATED_PRESENT_FIELD.matcher(expression).matches()) {
            return new UiFormula(expression.substring(2, expression.length() - 1));
        }
        return new UiFormula("!(" + expression + ")");
    }

    private static boolean isPortableBooleanExpression(String expression) {
        String normalized = expression.replaceAll("\\s+", "");
        if (PRESENT_FIELD.matcher(normalized).matches() || NEGATED_PRESENT_FIELD.matcher(normalized).matches()) {
            return true;
        }
        if (normalized.startsWith("!(") && normalized.endsWith(")")) {
            return isPortableBooleanExpression(normalized.substring(2, normalized.length() - 1));
        }
        String[] terms = normalized.split("&&", -1);
        return terms.length > 1 && java.util.Arrays.stream(terms).allMatch(term -> PRESENT_TERM.matcher(term).matches());
    }
}
