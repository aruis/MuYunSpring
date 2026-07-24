package net.ximatai.muyun.spring.ability.deletion;

import net.ximatai.muyun.spring.common.id.Ids;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Immutable context propagated through one explicit deletion tree.
 *
 * <p>The context deliberately carries resource identities rather than service
 * implementation details, so every child service remains free to choose its
 * own soft- or hard-delete strategy.</p>
 */
public record DeletionContext(String operationId,
                              DeletionResource root,
                              DeletionResource parent,
                              String parentEntryId,
                              DeletionTrigger trigger,
                              Set<DeletionResource> ancestry) {
    public DeletionContext {
        if (operationId == null || operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
        if (root == null) {
            throw new IllegalArgumentException("root must not be null");
        }
        if (trigger == null) {
            throw new IllegalArgumentException("trigger must not be null");
        }
        ancestry = ancestry == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(ancestry));
    }

    public static DeletionContext root(String moduleAlias, String recordId) {
        DeletionResource resource = new DeletionResource(moduleAlias, recordId);
        return new DeletionContext(Ids.newId(), resource, null, null, DeletionTrigger.DIRECT, Set.of(resource));
    }

    public void requireAllowed(DeletionResource resource) {
        if (ancestry.contains(resource)) {
            throw new IllegalStateException("cyclic deletion cascade: " + resource.moduleAlias() + "/" + resource.recordId());
        }
    }

    public DeletionContext child(DeletionNode node, String moduleAlias, String recordId) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }
        DeletionResource child = new DeletionResource(moduleAlias, recordId);
        requireAllowed(child);
        LinkedHashSet<DeletionResource> nextAncestry = new LinkedHashSet<>(ancestry);
        nextAncestry.add(child);
        return new DeletionContext(operationId, root, node.resource(), node.entryId(), DeletionTrigger.CASCADE, nextAncestry);
    }
}
