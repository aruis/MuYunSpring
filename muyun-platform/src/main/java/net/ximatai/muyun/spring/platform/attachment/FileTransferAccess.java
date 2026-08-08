package net.ximatai.muyun.spring.platform.attachment;

import java.time.Instant;
import java.util.Map;

/**
 * A browser or trusted backend transfer target issued for the current platform
 * identity.  This is deliberately not a file asset or a file metadata snapshot.
 */
public record FileTransferAccess(
        FileTransferOperation operation,
        String fileId,
        String accessToken,
        String url,
        Instant expiresAt,
        Map<String, String> formFields
) {
    public FileTransferAccess {
        formFields = formFields == null ? Map.of() : Map.copyOf(formFields);
    }

    /** Compatibility constructor for access contracts that carry all information in the URL. */
    public FileTransferAccess(FileTransferOperation operation,
                              String fileId,
                              String accessToken,
                              String url,
                              Instant expiresAt) {
        this(operation, fileId, accessToken, url, expiresAt, Map.of());
    }
}
