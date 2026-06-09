package net.ximatai.muyun.spring.platform.code;

public record CodeSequenceBaselineResult(
        CodeSequenceState state,
        Long beforeValue,
        Long afterValue,
        Long nextValue,
        String summary
) {
}
