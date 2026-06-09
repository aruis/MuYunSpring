package net.ximatai.muyun.spring.platform.exchange.importer;

public record DynamicImportResult(
        DynamicImportPlan plan,
        GroupedWorkbook groupedWorkbook,
        DynamicImportExecutionResult executionResult,
        byte[] errorWorkbookBytes
) {
    public DynamicImportResult {
        errorWorkbookBytes = errorWorkbookBytes == null ? null : errorWorkbookBytes.clone();
    }

    @Override
    public byte[] errorWorkbookBytes() {
        return errorWorkbookBytes == null ? null : errorWorkbookBytes.clone();
    }
}
