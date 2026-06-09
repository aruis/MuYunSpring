package net.ximatai.muyun.spring.platform.exchange.importer;

import java.util.LinkedHashMap;
import java.util.List;

public record GroupedWorkbook(
        LinkedHashMap<String, ImportGroup> groups,
        List<ImportErrorRow> errorRows
) {
    public GroupedWorkbook {
        groups = groups == null ? new LinkedHashMap<>() : new LinkedHashMap<>(groups);
        errorRows = errorRows == null ? List.of() : List.copyOf(errorRows);
    }
}
