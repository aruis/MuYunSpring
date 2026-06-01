package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.common.formula.FormulaRuntimeReport;

public record DynamicActionAvailability(
        String actionCode,
        boolean available,
        String message,
        FormulaRuntimeReport report
) {
    public DynamicActionAvailability {
        report = report == null ? new FormulaRuntimeReport() : report;
    }

    public static DynamicActionAvailability available(String actionCode) {
        return new DynamicActionAvailability(actionCode, true, null, new FormulaRuntimeReport());
    }

    public static DynamicActionAvailability unavailable(String actionCode, String message) {
        return new DynamicActionAvailability(actionCode, false, message, new FormulaRuntimeReport());
    }
}
