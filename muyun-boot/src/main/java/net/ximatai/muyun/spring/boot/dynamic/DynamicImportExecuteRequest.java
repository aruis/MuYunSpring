package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.spring.platform.exchange.importer.ImportDuplicateStrategy;

import java.util.List;

public record DynamicImportExecuteRequest(
        MainSheet mainSheet,
        List<ChildSheet> childSheets
) {
    public DynamicImportExecuteRequest {
        childSheets = childSheets == null ? List.of() : List.copyOf(childSheets);
    }

    public record MainSheet(
            String matchFieldName,
            ImportDuplicateStrategy duplicateStrategy
    ) {
    }

    public record ChildSheet(
            String entityAlias,
            String matchFieldName,
            ImportDuplicateStrategy duplicateStrategy
    ) {
    }
}
