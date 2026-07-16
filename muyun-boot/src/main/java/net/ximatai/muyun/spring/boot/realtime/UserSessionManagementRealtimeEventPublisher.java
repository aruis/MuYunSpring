package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.boot.platform.PlatformRecordActionAvailability;
import net.ximatai.muyun.spring.boot.platform.PlatformRecordActionAvailabilityService;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.user.UserSessionLifecycleEvent;
import net.ximatai.muyun.spring.iam.user.UserSessionLifecycleEventPublisher;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import java.security.Principal;
import java.util.Objects;
import java.util.Optional;

public class UserSessionManagementRealtimeEventPublisher implements UserSessionLifecycleEventPublisher {
    private static final String USER_MODULE_ALIAS = "iam.user";
    private static final String SESSIONS_ACTION = "sessions";

    private final SimpUserRegistry userRegistry;
    private final PlatformRecordActionAvailabilityService actionAvailabilityService;
    private final BusinessRealtimeNotifier businessRealtimeNotifier;
    private final UserSessionService userSessionService;

    public UserSessionManagementRealtimeEventPublisher(
            SimpUserRegistry userRegistry,
            PlatformRecordActionAvailabilityService actionAvailabilityService,
            BusinessRealtimeNotifier businessRealtimeNotifier,
            UserSessionService userSessionService) {
        this.userRegistry = Objects.requireNonNull(userRegistry, "userRegistry must not be null");
        this.actionAvailabilityService = Objects.requireNonNull(actionAvailabilityService,
                "actionAvailabilityService must not be null");
        this.businessRealtimeNotifier = Objects.requireNonNull(businessRealtimeNotifier,
                "businessRealtimeNotifier must not be null");
        this.userSessionService = Objects.requireNonNull(userSessionService, "userSessionService must not be null");
    }

    @Override
    public void publish(UserSessionLifecycleEvent event) {
        if (event == null) {
            return;
        }
        BusinessRealtimeEvent notification = BusinessRealtimeEvent.userSessionChanged(
                event.userId(), event.type().name());
        for (SimpUser user : userRegistry.getUsers()) {
            CurrentUser currentUser = currentUser(user.getPrincipal()).orElse(null);
            if (canReceive(currentUser, event.userId())) {
                businessRealtimeNotifier.notifyUser(currentUser.userId(), notification);
            }
        }
    }

    private boolean canReceive(CurrentUser currentUser, String targetUserId) {
        if (currentUser == null || currentUser.passwordChangeRequired()) {
            return false;
        }
        try (CurrentUserContext.Scope ignoredUser = CurrentUserContext.use(currentUser);
             TenantContext.Scope ignoredTenant = tenantScope(currentUser)) {
            PlatformRecordActionAvailability availability =
                    actionAvailabilityService.recordActions(USER_MODULE_ALIAS, targetUserId);
            return availability.actions().stream()
                    .anyMatch(action -> SESSIONS_ACTION.equals(action.actionCode()) && action.available());
        } catch (RuntimeException ignored) {
            return false;
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
            return TenantContext.system("user session management realtime notification");
        }
        String tenantId = currentUser.tenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return TenantContext.system("user session management realtime notification without tenant");
        }
        return TenantContext.use(tenantId);
    }
}
