package net.ximatai.muyun.spring.dynamic.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

public record DynamicReferenceResolveItem(
        String id,
        String title,
        DynamicReferenceMatchMode matchedBy,
        Map<String, Object> projections
) {
    public DynamicReferenceResolveItem {
        projections = projections == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(projections));
    }
}
