package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.ability.reference.ReferenceLoadPath;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;

import java.util.List;

/** Dynamic declaration compiled to the same typed read path used by static models. */
public record EntityReferenceLoadDefinition(
        String sourceEntityAlias,
        String sourceField,
        List<ReferenceLoadPath.Hop> hops,
        String terminalField,
        String outputField
) {
    public EntityReferenceLoadDefinition(String sourceEntityAlias,
                                         String sourceField,
                                         String terminalField,
                                         String outputField) {
        this(sourceEntityAlias, sourceField, List.of(), terminalField, outputField);
    }

    public EntityReferenceLoadDefinition {
        hops = hops == null ? List.of() : List.copyOf(hops);
    }

    public EntityReferenceLoadDefinition withHop(ReferenceTarget target, String viaField) {
        java.util.ArrayList<ReferenceLoadPath.Hop> next = new java.util.ArrayList<>(hops);
        next.add(new ReferenceLoadPath.Hop(target, viaField));
        return new EntityReferenceLoadDefinition(sourceEntityAlias, sourceField, next, terminalField, outputField);
    }

    public ReferenceLoadPath path(ReferenceTarget sourceTarget) {
        return new ReferenceLoadPath(sourceField, sourceTarget, hops, terminalField, outputField);
    }
}
