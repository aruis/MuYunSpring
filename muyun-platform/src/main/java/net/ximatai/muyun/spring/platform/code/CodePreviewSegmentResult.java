package net.ximatai.muyun.spring.platform.code;

public record CodePreviewSegmentResult(
        String segmentId,
        CodeSegmentType segmentType,
        String value,
        boolean sequenceBasis
) {
}
