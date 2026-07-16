package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import java.security.Principal;
import java.util.Objects;
import java.util.Optional;

public class OnlineUserBusinessRealtimeFanOutPublisher implements BusinessRealtimeFanOutPublisher {
    private final SimpUserRegistry userRegistry;
    private final UserSessionService userSessionService;
    private final BusinessRealtimeNotifier businessRealtimeNotifier;

    public OnlineUserBusinessRealtimeFanOutPublisher(
            SimpUserRegistry userRegistry,
            UserSessionService userSessionService,
            BusinessRealtimeNotifier businessRealtimeNotifier) {
        this.userRegistry = Objects.requireNonNull(userRegistry, "userRegistry must not be null");
        this.userSessionService = Objects.requireNonNull(userSessionService, "userSessionService must not be null");
        this.businessRealtimeNotifier = Objects.requireNonNull(
                businessRealtimeNotifier, "businessRealtimeNotifier must not be null");
    }

    @Override
    public void publish(BusinessRealtimeEvent event, RecipientPolicy recipientPolicy) {
        if (event == null || recipientPolicy == null) {
            return;
        }
        for (SimpUser user : userRegistry.getUsers()) {
            currentUser(user.getPrincipal()).ifPresent(currentUser -> notifyIfAllowed(event, currentUser,
                    recipientPolicy));
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
        } catch (RuntimeException ignored) {
            // Realtime fan-out must not break the source business lifecycle.
        }
    }

    private Optional<CurrentUser> currentUser(Principal principal) {
        if (principal instanceof CurrentUserPrincipal currentUserPrincipal) {
            return userSessionService.currentUser(currentUserPrincipal.token());
        }
        return Optional.empty();
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
