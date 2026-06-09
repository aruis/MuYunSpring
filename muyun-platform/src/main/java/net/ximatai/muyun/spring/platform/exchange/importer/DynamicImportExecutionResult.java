package net.ximatai.muyun.spring.platform.exchange.importer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DynamicImportExecutionResult(
        int created,
        int updated,
        int skipped,
        List<ImportErrorRow> errorRows,
        Map<String, ImportEntityExecutionSummary> summaries
) {
    public DynamicImportExecutionResult {
        errorRows = errorRows == null ? List.of() : List.copyOf(errorRows);
        summaries = summaries == null ? Map.of() : Map.copyOf(summaries);
    }

    static DynamicImportExecutionResult of(Map<String, ImportEntityExecutionSummary> summaries,
                                           List<ImportErrorRow> errorRows) {
        int created = 0;
        int updated = 0;
        int skipped = 0;
        LinkedHashMap<String, ImportEntityExecutionSummary> copiedSummaries = new LinkedHashMap<>(summaries);
        for (ImportEntityExecutionSummary summary : copiedSummaries.values()) {
            created += summary.created();
            updated += summary.updated();
            skipped += summary.skipped();
        }
        return new DynamicImportExecutionResult(created, updated, skipped, errorRows, copiedSummaries);
    }
}
