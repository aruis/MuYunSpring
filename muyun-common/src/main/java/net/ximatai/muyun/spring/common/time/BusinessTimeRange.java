package net.ximatai.muyun.spring.common.time;

import java.time.Instant;

public record BusinessTimeRange(
        Instant startInclusive,
        Instant endExclusive
) {
    public BusinessTimeRange {
        if (startInclusive == null) {
            throw new IllegalArgumentException("startInclusive must not be null");
        }
        if (endExclusive == null) {
            throw new IllegalArgumentException("endExclusive must not be null");
        }
        if (endExclusive.isBefore(startInclusive)) {
            throw new IllegalArgumentException("endExclusive must not be before startInclusive");
        }
    }
}
