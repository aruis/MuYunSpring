package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.iam.user.UserSessionLifecycleEvent;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UserSessionPresenceIdleNotifier {
    private final RealtimeConnectionRegistry connectionRegistry;
    private final UserSessionService userSessionService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;
    private final Set<String> idleSessionIds = ConcurrentHashMap.newKeySet();

    public UserSessionPresenceIdleNotifier(RealtimeConnectionRegistry connectionRegistry,
                                           UserSessionService userSessionService,
                                           ApplicationEventPublisher applicationEventPublisher) {
        this(connectionRegistry, userSessionService, applicationEventPublisher, Clock.systemUTC());
    }

    UserSessionPresenceIdleNotifier(RealtimeConnectionRegistry connectionRegistry,
                                    UserSessionService userSessionService,
                                    ApplicationEventPublisher applicationEventPublisher,
                                    Clock clock) {
        this.connectionRegistry = connectionRegistry;
        this.userSessionService = userSessionService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Scheduled(fixedDelayString = "${muyun.realtime.presence.idle-check-interval:60000}")
    public void publishIdleTransitions() {
        Instant now = clock.instant();
        Set<String> connectedSessionIds = ConcurrentHashMap.newKeySet();
        for (RealtimeConnectionRegistry.SessionPresenceEntry entry : connectionRegistry.sessionPresenceEntries()) {
            connectedSessionIds.add(entry.sessionId());
            if (!isIdle(entry, now)) {
                idleSessionIds.remove(entry.sessionId());
                continue;
            }
            if (idleSessionIds.add(entry.sessionId())) {
                applicationEventPublisher.publishEvent(
                        UserSessionLifecycleEvent.presenceIdle(entry.userId(), entry.sessionId()));
            }
        }
        idleSessionIds.retainAll(connectedSessionIds);
    }

    public void publishActiveIfIdle(CurrentUserPrincipal principal) {
        if (principal == null || principal.loginSessionId() == null) {
            return;
        }
        if (idleSessionIds.remove(principal.loginSessionId())) {
            applicationEventPublisher.publishEvent(UserSessionLifecycleEvent.presenceActive(
                    principal.currentUser().userId(), principal.loginSessionId()));
        }
    }

    private boolean isIdle(RealtimeConnectionRegistry.SessionPresenceEntry entry, Instant now) {
        return userSessionService.sessionIdleSince(entry.sessionId(), entry.lastObservedAt(), now);
    }
}
