package net.ximatai.muyun.spring.ability.deletion;

/** A deletion-log node allocated for one resource, if a lifecycle listener persists entries. */
public record DeletionNode(String entryId, DeletionResource resource) {
    public DeletionNode {
        if (resource == null) {
            throw new IllegalArgumentException("resource must not be null");
        }
    }

    public static DeletionNode transientNode(DeletionResource resource) {
        return new DeletionNode(null, resource);
    }
}
