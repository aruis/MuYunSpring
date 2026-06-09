package net.ximatai.muyun.spring.platform.exchange.importer;

import java.util.LinkedHashMap;

public record ImportErrorRow(
        String sheetKey,
        LinkedHashMap<String, String> rawValues,
        String message,
        String groupIdentity
) {
    public ImportErrorRow {
        rawValues = rawValues == null ? new LinkedHashMap<>() : new LinkedHashMap<>(rawValues);
    }

    public static ImportErrorRow of(ParsedImportRow row, String message, String groupIdentity) {
        return new ImportErrorRow(row.sheetKey(), row.rawValues(), message, groupIdentity);
    }
}
