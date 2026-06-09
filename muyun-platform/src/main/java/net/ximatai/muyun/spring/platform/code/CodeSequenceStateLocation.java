package net.ximatai.muyun.spring.platform.code;

public record CodeSequenceStateLocation(
        String ruleId,
        String basisKey,
        String periodKey,
        boolean found,
        CodeSequenceState state,
        Long nextValue,
        String summary
) {
}
