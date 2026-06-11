package net.ximatai.muyun.spring.dynamic.descriptor;

import java.util.List;

public record DynamicAssociationRelationOverview(
        String moduleAlias,
        List<DynamicAssociationRelationItem> upstream,
        List<DynamicAssociationRelationItem> downstream
) {
    public DynamicAssociationRelationOverview {
        upstream = upstream == null ? List.of() : List.copyOf(upstream);
        downstream = downstream == null ? List.of() : List.copyOf(downstream);
    }
}
