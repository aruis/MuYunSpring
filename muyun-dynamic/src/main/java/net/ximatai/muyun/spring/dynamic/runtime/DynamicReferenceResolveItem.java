package net.ximatai.muyun.spring.dynamic.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public record DynamicReferenceResolveItem(
        String id,
        String title,
        DynamicReferenceMatchMode matchedBy,
        Map<String, Object> projections,
        Map<String, Object> affectPatch
) {
    public DynamicReferenceResolveItem(String id,
                                       String title,
                                       DynamicReferenceMatchMode matchedBy,
                                       Map<String, Object> projections) {
        this(id, title, matchedBy, projections, Map.of());
    }

    public DynamicReferenceResolveItem {
        projections = projections == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(projections));
        affectPatch = affectPatch == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(affectPatch));
    }
}
