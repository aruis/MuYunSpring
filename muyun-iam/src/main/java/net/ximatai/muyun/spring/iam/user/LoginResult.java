package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.spring.common.identity.CurrentUser;

import java.time.Instant;

public record LoginResult(
        String token,
        String tokenType,
        String sessionId,
        Instant issuedAt,
        CurrentUser currentUser,
        boolean passwordChangeRequired,
        PasswordStatus passwordStatus,
        Instant passwordExpiresAt
) {
    public static LoginResult bearer(String token, String sessionId, Instant issuedAt, CurrentUser currentUser,
                                     boolean passwordChangeRequired, PasswordStatus passwordStatus,
                                     Instant passwordExpiresAt) {
        return new LoginResult(token, "Bearer", sessionId, issuedAt, currentUser,
                passwordChangeRequired, passwordStatus, passwordExpiresAt);
    }
}
