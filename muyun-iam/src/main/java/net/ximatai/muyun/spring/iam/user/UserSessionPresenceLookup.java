package net.ximatai.muyun.spring.iam.user;

public interface UserSessionPresenceLookup {
    UserSessionPresenceLookup NONE = UserSessionPresence::absent;

    UserSessionPresence presenceOf(String sessionId);
}
