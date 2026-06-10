package net.ximatai.muyun.spring.platform.attachment;

import java.util.Map;

public record RecordAttachmentAccess(
        String mode,
        String fileId,
        String accessToken,
        String url,
        String expiresAt,
        Map<String, Object> metadata
) {
    public RecordAttachmentAccess {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
