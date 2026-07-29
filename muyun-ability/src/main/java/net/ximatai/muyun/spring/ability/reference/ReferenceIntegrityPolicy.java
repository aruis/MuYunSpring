package net.ximatai.muyun.spring.ability.reference;

/** Resolved reference lifecycle policy shared by static and dynamic models. */
public record ReferenceIntegrityPolicy(
        ReferenceTargetDeletionPolicy onTargetSoftDelete
) {
    public static final ReferenceIntegrityPolicy DEFAULT = new ReferenceIntegrityPolicy(
            ReferenceTargetDeletionPolicy.PRESERVE_HISTORY);

    public ReferenceIntegrityPolicy {
        onTargetSoftDelete = onTargetSoftDelete == null
                ? ReferenceTargetDeletionPolicy.PRESERVE_HISTORY
                : onTargetSoftDelete;
    }

    public static ReferenceIntegrityPolicy from(ReferenceIntegrity integrity) {
        return integrity == null
                ? DEFAULT
                : new ReferenceIntegrityPolicy(integrity.onTargetSoftDelete());
    }
}
