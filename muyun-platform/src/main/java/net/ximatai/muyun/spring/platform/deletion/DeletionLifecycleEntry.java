package net.ximatai.muyun.spring.platform.deletion;

/** A terminal lifecycle entry together with its operation envelope. */
public record DeletionLifecycleEntry(DeletionOperation operation, DeletionEntry entry) {
}
