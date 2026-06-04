package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.spring.common.identity.CurrentUser;

import java.time.Instant;

public record LoginResult(
        String token,
        String tokenType,
        Instant issuedAt,
        CurrentUser currentUser
) {
    public static LoginResult bearer(String token, Instant issuedAt, CurrentUser currentUser) {
        return new LoginResult(token, "Bearer", issuedAt, currentUser);
    }
}
