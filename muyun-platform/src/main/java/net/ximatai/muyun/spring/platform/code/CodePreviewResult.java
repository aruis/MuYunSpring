package net.ximatai.muyun.spring.platform.code;

import java.util.List;

public record CodePreviewResult(
        String value,
        List<CodePreviewSegmentResult> segments
) {
}
