package net.ximatai.muyun.spring.common.formula;

import java.util.Locale;
import java.util.Set;

final class FormulaFunctions {
    private static final Set<String> AGGREGATE_FUNCTIONS = Set.of(
            "SUM",
            "AVG",
            "COUNT",
            "MAX",
            "MIN",
            "GET_FIRST_OR_DEFAULT_VALUE"
    );

    private FormulaFunctions() {
    }

    static boolean isAggregate(String name) {
        return AGGREGATE_FUNCTIONS.contains(normalize(name));
    }

    static String normalize(String name) {
        String normalized = name == null ? "" : name.toUpperCase(Locale.ROOT);
        if ("GETFIRSTORDEFAULTVALUE".equals(normalized)) {
            return "GET_FIRST_OR_DEFAULT_VALUE";
        }
        return normalized;
    }
}
