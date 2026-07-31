package net.ximatai.muyun.spring.iam.web.realtime;

import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import net.ximatai.muyun.spring.web.realtime.BusinessRealtimeEvent;
import net.ximatai.muyun.spring.web.realtime.BusinessRealtimeFanOutPublisher;
import net.ximatai.muyun.spring.web.realtime.BusinessRealtimeNotifier;
import net.ximatai.muyun.spring.web.realtime.CurrentUserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class OnlineUserBusinessRealtimeFanOutPublisher implements BusinessRealtimeFanOutPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnlineUserBusinessRealtimeFanOutPublisher.class);

    private final RealtimeConnectionRegistry connectionRegistry;
    private final UserSessionService userSessionService;
    private final BusinessRealtimeNotifier businessRealtimeNotifier;

    public OnlineUserBusinessRealtimeFanOutPublisher(
            RealtimeConnectionRegistry connectionRegistry,
            UserSessionService userSessionService,
            BusinessRealtimeNotifier businessRealtimeNotifier) {
        this.connectionRegistry = Objects.requireNonNull(connectionRegistry, "connectionRegistry must not be null");
        this.userSessionService = Objects.requireNonNull(userSessionService, "userSessionService must not be null");
        this.businessRealtimeNotifier = Objects.requireNonNull(
                businessRealtimeNotifier, "businessRealtimeNotifier must not be null");
    }

    @Override
    public void publish(BusinessRealtimeEvent event, RecipientPolicy recipientPolicy) {
        if (event == null || recipientPolicy == null) {
            return;
        }
        Set<String> notifiedUserIds = new HashSet<>();
        for (CurrentUserPrincipal principal : connectionRegistry.principals()) {
            Optional<CurrentUser> currentUser = currentUser(principal);
            if (currentUser.isEmpty()) {
                LOGGER.debug("Skip business realtime event for stale realtime connection: userId={}, eventType={}, "
                                + "recordId={}",
                        principal.currentUser().userId(), event.type(), event.recordId());
                continue;
            }
            currentUser
                    .filter(user -> notifiedUserIds.add(user.userId()))
                    .ifPresent(user -> notifyIfAllowed(event, user, recipientPolicy));
        }
    }

    private void notifyIfAllowed(BusinessRealtimeEvent event, CurrentUser currentUser,
                                 RecipientPolicy recipientPolicy) {
        if (currentUser.passwordChangeRequired()) {
            return;
        }
        try (CurrentUserContext.Scope ignoredUser = CurrentUserContext.use(currentUser);
             TenantContext.Scope ignoredTenant = tenantScope(currentUser)) {
            if (recipientPolicy.canReceive(currentUser)) {
                businessRealtimeNotifier.notifyUser(currentUser.userId(), event);
            }
        } catch (RuntimeException exception) {
            // Realtime fan-out must not break the source business lifecycle.
            LOGGER.debug("Failed to fan-out business realtime event: userId={}, eventType={}, recordId={}",
                    currentUser.userId(), event.type(), event.recordId(), exception);
        }
    }

    private Optional<CurrentUser> currentUser(CurrentUserPrincipal principal) {
        if (principal == null) {
            return Optional.empty();
        }
        try (TenantContext.Scope ignored = TenantContext.bypassTenantFilter(
                "business realtime fan-out principal lookup")) {
            return userSessionService.currentUserSnapshot(principal.token());
        }
    }

    private TenantContext.Scope tenantScope(CurrentUser currentUser) {
        if (currentUser.system()) {
            return TenantContext.system("business realtime fan-out");
        }
        String tenantId = currentUser.tenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return TenantContext.system("business realtime fan-out without tenant");
        }
        return TenantContext.use(tenantId);
    }
}
