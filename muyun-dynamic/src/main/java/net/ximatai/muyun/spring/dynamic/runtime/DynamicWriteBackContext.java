package net.ximatai.muyun.spring.dynamic.runtime;

import java.util.UUID;

public record DynamicWriteBackContext(
        String traceId,
        int depth,
        String parentExecutionId,
        boolean cascadeAllowed
) {
    private static final int MAX_DEPTH = 5;

    public DynamicWriteBackContext {
        traceId = traceId == null || traceId.isBlank() ? UUID.randomUUID().toString() : traceId;
        if (depth < 0) {
            throw new IllegalArgumentException("write-back depth must not be negative");
        }
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("write-back depth exceeds limit: " + depth);
        }
        parentExecutionId = parentExecutionId == null || parentExecutionId.isBlank() ? null : parentExecutionId;
    }

    public static DynamicWriteBackContext root() {
        return new DynamicWriteBackContext(null, 0, null, false);
    }

    public DynamicWriteBackContext next(String parentExecutionId, boolean cascadeAllowed) {
        return new DynamicWriteBackContext(traceId, depth + 1, parentExecutionId, cascadeAllowed);
    }
}
