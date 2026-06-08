package net.ximatai.muyun.spring.platform.code;

public record CodeSequenceAllocation(
        String ruleId,
        String basisKey,
        String periodKey,
        String tenantId,
        long startValue,
        long stepValue,
        Integer sequenceLength,
        Long maxValue,
        CodeSequenceOverflowPolicy overflowPolicy
) {
}
