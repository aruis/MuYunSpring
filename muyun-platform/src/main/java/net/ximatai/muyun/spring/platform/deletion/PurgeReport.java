package net.ximatai.muyun.spring.platform.deletion;

import java.util.List;

/** Structured outcome of purging one source delete operation. */
public record PurgeReport(String sourceOperationId, String purgeOperationId, List<PurgeEntryResult> entries) {
    public PurgeReport {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }
}
