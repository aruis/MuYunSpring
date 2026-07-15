package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.iam.user.UserSecurityEvent;
import net.ximatai.muyun.spring.iam.user.UserSecurityEventPublisher;
import net.ximatai.muyun.spring.iam.user.UserSessionService;

public class UserSecurityRealtimeEventPublisher implements UserSecurityEventPublisher {
    private final UserSessionService userSessionService;
    private final SecurityRealtimeNotifier securityRealtimeNotifier;

    public UserSecurityRealtimeEventPublisher(UserSessionService userSessionService,
                                              SecurityRealtimeNotifier securityRealtimeNotifier) {
        this.userSessionService = userSessionService;
        this.securityRealtimeNotifier = securityRealtimeNotifier;
    }

    @Override
    public void publish(UserSecurityEvent event) {
        if (event == null) {
            return;
        }
        userSessionService.revokeUserSessions(event.userId());
        switch (event.type()) {
            case PASSWORD_CHANGED -> securityRealtimeNotifier.notifyPasswordChanged(event.userId());
            case PASSWORD_RESET -> securityRealtimeNotifier.notifyPasswordReset(event.userId());
            case FORCE_LOGOUT -> securityRealtimeNotifier.notifyForceLogout(event.userId());
        }
    }
}
