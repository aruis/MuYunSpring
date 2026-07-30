package net.ximatai.muyun.spring.ability.reference;

/** Resolved reference lifecycle policy shared by static and dynamic models. */
public record ReferenceIntegrityPolicy(
        ReferenceTargetUnavailablePolicy onTargetUnavailable
) {
    public static final ReferenceIntegrityPolicy DEFAULT = new ReferenceIntegrityPolicy(
            ReferenceTargetUnavailablePolicy.PRESERVE_HISTORY);

    public ReferenceIntegrityPolicy {
        onTargetUnavailable = onTargetUnavailable == null
                ? ReferenceTargetUnavailablePolicy.PRESERVE_HISTORY
                : onTargetUnavailable;
    }

    public static ReferenceIntegrityPolicy from(ReferenceIntegrity integrity) {
        if (integrity == null) {
            return DEFAULT;
        }
        ReferenceTargetUnavailablePolicy unavailable = integrity.onTargetUnavailable();
        if (unavailable == ReferenceTargetUnavailablePolicy.PRESERVE_HISTORY
                && integrity.onTargetSoftDelete() == ReferenceTargetDeletionPolicy.RESTRICT) {
            unavailable = ReferenceTargetUnavailablePolicy.RESTRICT;
        }
        return new ReferenceIntegrityPolicy(unavailable);
    }
}
