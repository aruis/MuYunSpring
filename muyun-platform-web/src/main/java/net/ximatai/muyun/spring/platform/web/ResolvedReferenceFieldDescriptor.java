package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;

/** Client-safe target metadata for a static entity reference form field. */
public record ResolvedReferenceFieldDescriptor(String targetModuleAlias,
                                               ReferenceCardinality cardinality) {
    public ResolvedReferenceFieldDescriptor {
        targetModuleAlias = PlatformNameRules.requireModuleAlias(targetModuleAlias);
        cardinality = cardinality == null ? ReferenceCardinality.ONE : cardinality;
    }
}
