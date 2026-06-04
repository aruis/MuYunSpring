package net.ximatai.muyun.spring.common.platform;

public enum ActionDefaultGrantPolicy {
    NONE(false),
    ANY_LOGIN_USER(true),
    OWNER(true),
    ASSIGNEE(true),
    MEMBER(true);

    private final boolean grantsAuthenticatedUser;

    ActionDefaultGrantPolicy(boolean grantsAuthenticatedUser) {
        this.grantsAuthenticatedUser = grantsAuthenticatedUser;
    }

    public boolean grantsAuthenticatedUser() {
        return grantsAuthenticatedUser;
    }

    public boolean requiresDataScope() {
        return this == OWNER || this == ASSIGNEE || this == MEMBER;
    }
}
