package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.common.formula.FormulaRuntimeReport;

import java.util.List;

public record DynamicFormulaPreviewResult(
        DynamicRecord record,
        FormulaRuntimeReport report,
        List<String> changedFields
) {
    public DynamicFormulaPreviewResult {
        report = report == null ? new FormulaRuntimeReport() : report;
        changedFields = changedFields == null ? List.of() : List.copyOf(changedFields);
    }
}
