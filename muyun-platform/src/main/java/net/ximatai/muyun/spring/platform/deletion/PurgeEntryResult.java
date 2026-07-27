package net.ximatai.muyun.spring.platform.deletion;

/** One resource result in a best-effort recycle-bin purge tree. */
public record PurgeEntryResult(String sourceEntryId, String moduleAlias, String recordId,
                               Status status, String message) {
    public enum Status { PURGED, SKIPPED, FAILED }
}
