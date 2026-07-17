package net.ximatai.muyun.spring.iam.user;

public record UserSessionStatusView(
        String userId,
        boolean online,
        long activeSessionCount,
        boolean present,
        long presentSessionCount,
        long idleSessionCount
) {
}
