package net.ximatai.muyun.spring.dynamic.runtime;

import java.util.List;

public record DynamicReferenceResolveResult(
        Object input,
        DynamicReferenceResolveStatus status,
        DynamicReferenceMatchMode matchedBy,
        DynamicReferenceResolveItem item,
        List<DynamicReferenceResolveItem> candidates
) {
    public DynamicReferenceResolveResult {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }
}
