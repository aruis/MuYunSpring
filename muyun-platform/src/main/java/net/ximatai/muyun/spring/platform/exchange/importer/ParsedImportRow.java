package net.ximatai.muyun.spring.platform.exchange.importer;

import java.util.LinkedHashMap;

public record ParsedImportRow(
        String sheetKey,
        LinkedHashMap<String, String> rawValues,
        LinkedHashMap<String, String> valuesByFieldName,
        LinkedHashMap<String, Object> convertedValues
) {
    public ParsedImportRow(String sheetKey,
                           LinkedHashMap<String, String> rawValues,
                           LinkedHashMap<String, String> valuesByFieldName) {
        this(sheetKey, rawValues, valuesByFieldName, new LinkedHashMap<>());
    }

    public ParsedImportRow {
        rawValues = rawValues == null ? new LinkedHashMap<>() : new LinkedHashMap<>(rawValues);
        valuesByFieldName = valuesByFieldName == null ? new LinkedHashMap<>() : new LinkedHashMap<>(valuesByFieldName);
        convertedValues = convertedValues == null ? new LinkedHashMap<>() : new LinkedHashMap<>(convertedValues);
    }
}
