package net.ximatai.muyun.spring.common.formula;

import java.util.Locale;
import java.util.Set;

final class FormulaFunctions {
    private static final Set<String> AGGREGATE_FUNCTIONS = Set.of("SUM", "AVG", "COUNT", "MAX", "MIN");

    private FormulaFunctions() {
    }

    static boolean isAggregate(String name) {
        return AGGREGATE_FUNCTIONS.contains(normalize(name));
    }

    static String normalize(String name) {
        return name == null ? "" : name.toUpperCase(Locale.ROOT);
    }
}
