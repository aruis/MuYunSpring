package net.ximatai.muyun.spring.platform.deletion;

import java.util.List;

/** Structured outcome of restoring one source delete operation. */
public record RestoreReport(String sourceOperationId, String restoreOperationId, List<RestoreEntryResult> entries) {
    public RestoreReport {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }
}
