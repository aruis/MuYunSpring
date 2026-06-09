package net.ximatai.muyun.spring.platform.exchange.importer;

import java.util.List;

public record DynamicImportParseResult(
        String moduleAlias,
        String mainEntityAlias,
        String mainSheetName,
        List<Sheet> sheets
) {
    public DynamicImportParseResult {
        if (mainEntityAlias == null || mainEntityAlias.isBlank()) {
            throw new IllegalArgumentException("dynamic import parse result requires mainEntityAlias");
        }
        sheets = sheets == null ? List.of() : List.copyOf(sheets);
    }

    public record Sheet(
            String sheetKey,
            String sheetName,
            String entityAlias,
            boolean main,
            int rowCount,
            List<Field> fields
    ) {
        public Sheet {
            fields = fields == null ? List.of() : List.copyOf(fields);
        }
    }

    public record Field(
            String fieldName,
            String title,
            boolean relateId,
            boolean matchKeyCandidate
    ) {
    }
}
