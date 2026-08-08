package net.ximatai.muyun.spring.platform.attachment;

import java.util.Map;

public record RecordAttachmentAccess(
        String mode,
        String fileId,
        String accessToken,
        String url,
        String expiresAt,
        Map<String, String> formFields,
        Map<String, Object> metadata
) {
    public RecordAttachmentAccess {
        formFields = formFields == null ? Map.of() : Map.copyOf(formFields);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /** Compatibility constructor for access providers without multipart form fields. */
    public RecordAttachmentAccess(String mode,
                                  String fileId,
                                  String accessToken,
                                  String url,
                                  String expiresAt,
                                  Map<String, Object> metadata) {
        this(mode, fileId, accessToken, url, expiresAt, Map.of(), metadata);
    }
}
