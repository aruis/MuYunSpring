package net.ximatai.muyun.spring.ability.reference;

/**
 * Defines whether an active referrer can tolerate its target no longer being
 * available for normal reference resolution.
 */
public enum ReferenceTargetUnavailablePolicy {
    PRESERVE_HISTORY,
    RESTRICT,
    CASCADE_DELETE
}
