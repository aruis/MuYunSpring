package net.ximatai.muyun.spring.dynamic.runtime;

import java.util.List;

public record DynamicReferenceResolveResponse(
        DynamicReferenceResolveStatus status,
        DynamicReferenceResolveMode mode,
        List<DynamicReferenceResolveItem> options,
        List<DynamicReferenceResolveResult> results,
        int offset,
        int limit,
        long total
) {
    public DynamicReferenceResolveResponse {
        options = options == null ? List.of() : List.copyOf(options);
        results = results == null ? List.of() : List.copyOf(results);
    }
}
