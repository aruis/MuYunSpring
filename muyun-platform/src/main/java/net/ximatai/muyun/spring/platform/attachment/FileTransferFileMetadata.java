package net.ximatai.muyun.spring.platform.attachment;

import java.time.Instant;

/**
 * File metadata read from the configured transfer provider.
 *
 * <p>This is a transport snapshot, not a platform file asset.  The file server
 * remains the only owner of this metadata and applications persist only the
 * business facts they need (normally the {@code fileId}).</p>
 */
public record FileTransferFileMetadata(
        String fileId,
        String originalFilename,
        String extension,
        String mimeType,
        long sizeBytes,
        String sha256,
        String status,
        boolean temporary,
        Instant uploadedAt
) {
}
