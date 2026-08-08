package net.ximatai.muyun.spring.platform.attachment;

import java.time.Instant;
/**
 * A browser or trusted backend transfer target issued for the current platform
 * identity.  This is deliberately not a file asset or a file metadata snapshot.
 */
public record FileTransferAccess(
        FileTransferOperation operation,
        String fileId,
        String accessToken,
        String url,
        Instant expiresAt
) {
}
