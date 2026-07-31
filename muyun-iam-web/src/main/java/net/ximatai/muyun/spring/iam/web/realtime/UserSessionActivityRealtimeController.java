package net.ximatai.muyun.spring.iam.web.realtime;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import net.ximatai.muyun.spring.web.realtime.CurrentUserPrincipal;

import java.security.Principal;

@Controller
public class UserSessionActivityRealtimeController {
    private final UserSessionPresenceIdleNotifier presenceIdleNotifier;

    public UserSessionActivityRealtimeController(UserSessionPresenceIdleNotifier presenceIdleNotifier) {
        this.presenceIdleNotifier = presenceIdleNotifier;
    }

    @MessageMapping("/platform/session/activity")
    public void recordActivity(Principal principal) {
        if (principal instanceof CurrentUserPrincipal currentUserPrincipal) {
            presenceIdleNotifier.publishActiveIfIdle(currentUserPrincipal);
        }
    }
}
