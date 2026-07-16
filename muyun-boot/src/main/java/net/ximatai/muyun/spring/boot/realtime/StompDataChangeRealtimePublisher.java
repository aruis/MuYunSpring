package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.ability.action.CommittedChangeSet;
import net.ximatai.muyun.spring.ability.action.DataChange;
import net.ximatai.muyun.spring.boot.platform.PlatformRecordActionAvailability;
import net.ximatai.muyun.spring.boot.platform.PlatformRecordActionAvailabilityService;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.web.RequestTraceContext;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class StompDataChangeRealtimePublisher implements DataChangeRealtimePublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(StompDataChangeRealtimePublisher.class);
    public static final String MESSAGE_TYPE = "platform.data-change";

    private final RealtimeMessagePublisher messagePublisher;
    private final RealtimeConnectionRegistry connectionRegistry;
    private final UserSessionService userSessionService;
    private final Supplier<PlatformRecordActionAvailabilityService> actionAvailabilityService;

    public StompDataChangeRealtimePublisher(RealtimeMessagePublisher messagePublisher) {
        this(messagePublisher, null, null, () -> null);
    }

    public StompDataChangeRealtimePublisher(RealtimeMessagePublisher messagePublisher,
                                            RealtimeConnectionRegistry connectionRegistry,
                                            UserSessionService userSessionService,
                                            Supplier<PlatformRecordActionAvailabilityService> actionAvailabilityService) {
        this.messagePublisher = messagePublisher;
        this.connectionRegistry = connectionRegistry;
        this.userSessionService = userSessionService;
        this.actionAvailabilityService = actionAvailabilityService == null ? () -> null : actionAvailabilityService;
    }

    @Override
    public void publish(CommittedChangeSet changeSet) {
        if (changeSet == null || changeSet.changes().isEmpty()) {
            return;
        }
        CurrentUser currentUser = CurrentUserContext.currentUser().orElse(null);
        if (currentUser == null) {
            return;
        }
        String traceId = RequestTraceContext.currentTraceId().orElse(null);
        RealtimeEnvelope<CommittedChangeSet> envelope = RealtimeEnvelope.of(MESSAGE_TYPE, traceId, changeSet);
        messagePublisher.sendToUser(currentUser.userId(), RealtimeDestinations.DATA_CHANGES, envelope);
        fanOutChangeSet(currentUser, changeSet, traceId);
    }

    private void fanOutChangeSet(CurrentUser sourceUser, CommittedChangeSet changeSet, String traceId) {
        if (connectionRegistry == null || userSessionService == null) {
            return;
        }
        for (CurrentUserPrincipal principal : connectionRegistry.principals()) {
            Optional<CurrentUser> currentUser = currentUser(principal);
            if (currentUser.isEmpty() || Objects.equals(currentUser.get().userId(), sourceUser.userId())) {
                continue;
            }
            notifyIfAllowed(currentUser.get(), changeSet, traceId);
        }
    }

    private void notifyIfAllowed(CurrentUser currentUser, CommittedChangeSet changeSet, String traceId) {
        if (currentUser.passwordChangeRequired()) {
            return;
        }
        try (CurrentUserContext.Scope ignoredUser = CurrentUserContext.use(currentUser);
             TenantContext.Scope ignoredTenant = tenantScope(currentUser)) {
            List<DataChange> visibleChanges = changeSet.changes().stream()
                    .filter(this::canReceive)
                    .toList();
            if (visibleChanges.isEmpty()) {
                return;
            }
            CommittedChangeSet visibleChangeSet = new CommittedChangeSet(changeSet.changeSetId(), visibleChanges);
            messagePublisher.sendToUser(currentUser.userId(), RealtimeDestinations.DATA_CHANGES,
                    RealtimeEnvelope.of(MESSAGE_TYPE, traceId, visibleChangeSet));
        } catch (RuntimeException exception) {
            LOGGER.debug("Failed to fan-out data change realtime event: userId={}, changeSetId={}",
                    currentUser.userId(), changeSet.changeSetId(), exception);
        }
    }

    private boolean canReceive(DataChange change) {
        if (change == null || change.recordId() == null) {
            return false;
        }
        PlatformRecordActionAvailabilityService availabilityService = actionAvailabilityService.get();
        if (availabilityService == null) {
            return false;
        }
        try {
            PlatformRecordActionAvailability availability =
                    availabilityService.recordActions(change.moduleAlias(), change.recordId());
            return availability.actions().stream()
                    .anyMatch(action -> PlatformAction.VIEW.code().equals(action.actionCode()) && action.available());
        } catch (RuntimeException exception) {
            LOGGER.debug("Failed to evaluate data change realtime recipient: moduleAlias={}, recordId={}",
                    change.moduleAlias(), change.recordId(), exception);
            return false;
        }
    }

    private Optional<CurrentUser> currentUser(CurrentUserPrincipal principal) {
        if (principal == null) {
            return Optional.empty();
        }
        try (TenantContext.Scope ignored = TenantContext.bypassTenantFilter(
                "data change realtime fan-out principal lookup")) {
            return userSessionService.currentUserSnapshot(principal.token());
        }
    }

    private TenantContext.Scope tenantScope(CurrentUser currentUser) {
        if (currentUser.system()) {
            return TenantContext.system("data change realtime fan-out");
        }
        String tenantId = currentUser.tenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return TenantContext.system("data change realtime fan-out without tenant");
        }
        return TenantContext.use(tenantId);
    }
}
