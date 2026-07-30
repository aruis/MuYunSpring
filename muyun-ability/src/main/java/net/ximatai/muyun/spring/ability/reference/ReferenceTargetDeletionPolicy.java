package net.ximatai.muyun.spring.ability.reference;

/**
 * @deprecated Use {@link ReferenceTargetUnavailablePolicy}. Kept only for
 * source compatibility while clients migrate their annotations.
 */
@Deprecated(since = "0.1", forRemoval = false)
public enum ReferenceTargetDeletionPolicy {
    PRESERVE_HISTORY,
    RESTRICT
}
