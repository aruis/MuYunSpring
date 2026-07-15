package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.iam.user.UserSecurityEvent;
import net.ximatai.muyun.spring.iam.user.UserSecurityEventPublisher;

public class UserSecurityRealtimeEventPublisher implements UserSecurityEventPublisher {
    private final SecurityRealtimeNotifier securityRealtimeNotifier;

    public UserSecurityRealtimeEventPublisher(SecurityRealtimeNotifier securityRealtimeNotifier) {
        this.securityRealtimeNotifier = securityRealtimeNotifier;
    }

    @Override
    public void publish(UserSecurityEvent event) {
        if (event == null) {
            return;
        }
        switch (event.type()) {
            case PASSWORD_CHANGED -> securityRealtimeNotifier.notifyPasswordChanged(event.userId());
            case PASSWORD_RESET -> securityRealtimeNotifier.notifyPasswordReset(event.userId());
            case FORCE_LOGOUT -> securityRealtimeNotifier.notifyForceLogout(event.userId());
            case SESSION_REVOKED -> securityRealtimeNotifier.notifySessionRevoked(event.userId(), event.sessionId());
        }
    }
}
