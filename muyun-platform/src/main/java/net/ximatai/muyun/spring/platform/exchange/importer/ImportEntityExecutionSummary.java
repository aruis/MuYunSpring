package net.ximatai.muyun.spring.platform.exchange.importer;

public record ImportEntityExecutionSummary(
        String entityAlias,
        int created,
        int updated,
        int skipped,
        int errors
) {
    public ImportEntityExecutionSummary addCreated() {
        return new ImportEntityExecutionSummary(entityAlias, created + 1, updated, skipped, errors);
    }

    public ImportEntityExecutionSummary addUpdated() {
        return new ImportEntityExecutionSummary(entityAlias, created, updated + 1, skipped, errors);
    }

    public ImportEntityExecutionSummary addSkipped() {
        return new ImportEntityExecutionSummary(entityAlias, created, updated, skipped + 1, errors);
    }

    public ImportEntityExecutionSummary addError() {
        return new ImportEntityExecutionSummary(entityAlias, created, updated, skipped, errors + 1);
    }
}
