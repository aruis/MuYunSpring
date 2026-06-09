package net.ximatai.muyun.spring.platform.exchange.importer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ImportGroup {
    private final String groupKey;
    private final ParsedImportRow mainRow;
    private final Map<String, List<ParsedImportRow>> childRowsBySheetKey = new LinkedHashMap<>();
    private final Set<String> coveredChildSheetKeys = new LinkedHashSet<>();

    ImportGroup(String groupKey, ParsedImportRow mainRow) {
        this.groupKey = groupKey;
        this.mainRow = mainRow;
    }

    public String groupKey() {
        return groupKey;
    }

    public ParsedImportRow mainRow() {
        return mainRow;
    }

    public Map<String, List<ParsedImportRow>> childRowsBySheetKey() {
        return childRowsBySheetKey;
    }

    public void addChild(String sheetKey, ParsedImportRow row) {
        coveredChildSheetKeys.add(sheetKey);
        childRowsBySheetKey.computeIfAbsent(sheetKey, key -> new ArrayList<>()).add(row);
    }

    public void markChildSheetCovered(String sheetKey) {
        coveredChildSheetKeys.add(sheetKey);
    }

    public boolean isChildSheetCovered(String sheetKey) {
        return coveredChildSheetKeys.contains(sheetKey);
    }
}
