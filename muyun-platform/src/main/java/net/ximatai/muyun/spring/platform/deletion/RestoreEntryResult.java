package net.ximatai.muyun.spring.platform.deletion;

/** One resource result in a best-effort restore tree. */
public record RestoreEntryResult(String sourceEntryId, String moduleAlias, String recordId,
                                 Status status, String message) {
    public enum Status { RESTORED, SKIPPED, FAILED }
}
