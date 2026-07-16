package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.ability.action.CommittedChangeSet;
import net.ximatai.muyun.spring.ability.action.DataChange;
import net.ximatai.muyun.spring.boot.platform.PlatformRecordActionAvailability;
import net.ximatai.muyun.spring.boot.platform.PlatformRecordActionAvailabilityService;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.web.RequestTraceContext;
import net.ximatai.muyun.spring.iam.user.UserSecurityEvent;
import net.ximatai.muyun.spring.iam.user.UserSessionLifecycleEvent;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.MessageBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RealtimePublisherTest {
    @Test
    void shouldSendDataChangeEnvelopeToCurrentUserQueue() {
        RecordingRealtimeMessagePublisher messagePublisher = new RecordingRealtimeMessagePublisher();
        StompDataChangeRealtimePublisher publisher = new StompDataChangeRealtimePublisher(messagePublisher);
        CommittedChangeSet changeSet = new CommittedChangeSet("change-set-1",
                List.of(DataChange.recordUpdated("iam.employee", "employee-1")));

        try (CurrentUserContext.Scope ignored = CurrentUserContext.use(
                CurrentUser.tenantUser("user-1", "User", "tenant-a"))) {
            publisher.publish(changeSet);
        }

        assertThat(messagePublisher.userId).isEqualTo("user-1");
        assertThat(messagePublisher.queue).isEqualTo(RealtimeDestinations.DATA_CHANGES);
        assertThat(messagePublisher.payload).isInstanceOf(RealtimeEnvelope.class);
        RealtimeEnvelope<?> envelope = (RealtimeEnvelope<?>) messagePublisher.payload;
        assertThat(envelope.type()).isEqualTo(StompDataChangeRealtimePublisher.MESSAGE_TYPE);
        assertThat(envelope.payload()).isEqualTo(changeSet);
    }

    @Test
    void shouldSkipDataChangeWithoutCurrentUser() {
        RecordingRealtimeMessagePublisher messagePublisher = new RecordingRealtimeMessagePublisher();
        StompDataChangeRealtimePublisher publisher = new StompDataChangeRealtimePublisher(messagePublisher);

        publisher.publish(new CommittedChangeSet("change-set-1",
                List.of(DataChange.recordUpdated("iam.employee", "employee-1"))));

        assertThat(messagePublisher.payload).isNull();
    }

    @Test
    void shouldSkipEmptyChangeSet() {
        RecordingRealtimeMessagePublisher messagePublisher = new RecordingRealtimeMessagePublisher();
        StompDataChangeRealtimePublisher publisher = new StompDataChangeRealtimePublisher(messagePublisher);

        publisher.publish(CommittedChangeSet.empty("change-set-1"));

        assertThat(messagePublisher.payload).isNull();
    }

    @Test
    void shouldBuildStandardRealtimeDestinations() {
        assertThat(RealtimeDestinations.USER_IM_MESSAGES.destination()).isEqualTo("/queue/platform/im/messages");
        assertThat(RealtimeDestinations.IM_MESSAGES_SEND.destination()).isEqualTo("/app/platform/im/messages/send");
        assertThat(RealtimeDestinations.tenantPublicDataChanges("tenant-a").destination())
                .isEqualTo("/topic/platform/tenants/tenant-a/public/data-changes");
        assertThat(RealtimeDestinations.tenantPublicNotifications("tenant-a").destination())
                .isEqualTo("/topic/platform/tenants/tenant-a/public/notifications");
        assertThat(RealtimeDestinations.organizationPublicDataChanges("org-1").destination())
                .isEqualTo("/topic/platform/organizations/org-1/public/data-changes");
        assertThat(RealtimeDestinations.organizationPublicNotifications("org-1").destination())
                .isEqualTo("/topic/platform/organizations/org-1/public/notifications");
        assertThat(RealtimeDestinations.moduleDataChanges("iam.employee").destination())
                .isEqualTo("/topic/platform/modules/iam.employee/data-changes");
        assertThat(RealtimeDestinations.recordDataChanges("iam.employee", "employee-1").destination())
                .isEqualTo("/topic/platform/modules/iam.employee/records/employee-1/data-changes");
        assertThat(RealtimeDestinations.resourceDataChanges("iam.employee", "children").destination())
                .isEqualTo("/topic/platform/modules/iam.employee/resources/children/data-changes");
        assertThat(RealtimeDestinations.resourceRecordDataChanges("iam.employee", "children", "employee-1")
                .destination()).isEqualTo(
                        "/topic/platform/modules/iam.employee/resources/children/records/employee-1/data-changes");
        assertThat(RealtimeDestinations.contextDataChanges("workflow", "task-1").destination())
                .isEqualTo("/topic/platform/contexts/workflow/task-1/data-changes");
        assertThat(RealtimeDestinations.imConversationMessages("conversation-1").destination())
                .isEqualTo("/topic/platform/im/conversations/conversation-1/messages");
    }

    @Test
    void shouldEncodeRealtimeDestinationPathSegments() {
        assertThat(RealtimeDestinations.recordDataChanges("order/form", "record 1").destination())
                .isEqualTo("/topic/platform/modules/order%2Fform/records/record%201/data-changes");

        assertThatThrownBy(() -> RealtimeDestinations.moduleDataChanges(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("realtime destination path segment must not be blank");
    }

    @Test
    void shouldSendPasswordSecurityNotificationsToUserQueue() {
        RecordingRealtimeMessagePublisher messagePublisher = new RecordingRealtimeMessagePublisher();
        StompSecurityRealtimeNotifier notifier = new StompSecurityRealtimeNotifier(messagePublisher);

        try (RequestTraceContext.Scope ignored = RequestTraceContext.use("trace-1")) {
            notifier.notifyPasswordReset("user-1");
        }

        assertThat(messagePublisher.userId).isEqualTo("user-1");
        assertThat(messagePublisher.queue).isEqualTo(RealtimeDestinations.USER_NOTIFICATIONS);
        assertThat(messagePublisher.payload).isInstanceOf(RealtimeEnvelope.class);
        RealtimeEnvelope<?> envelope = (RealtimeEnvelope<?>) messagePublisher.payload;
        assertThat(envelope.type()).isEqualTo(StompSecurityRealtimeNotifier.MESSAGE_TYPE);
        assertThat(envelope.traceId()).isEqualTo("trace-1");
        assertThat(envelope.payload()).isEqualTo(SecurityNotification.passwordReset());
    }

    @Test
    void shouldSendBusinessEventsToUserQueue() {
        RecordingRealtimeMessagePublisher messagePublisher = new RecordingRealtimeMessagePublisher();
        StompBusinessRealtimeNotifier notifier = new StompBusinessRealtimeNotifier(messagePublisher);
        BusinessRealtimeEvent event = BusinessRealtimeEvent.userSessionChanged("user-1", "LOGGED_IN");

        try (RequestTraceContext.Scope ignored = RequestTraceContext.use("trace-1")) {
            notifier.notifyUser("admin-1", event);
        }

        assertThat(messagePublisher.userId).isEqualTo("admin-1");
        assertThat(messagePublisher.queue).isEqualTo(RealtimeDestinations.USER_BUSINESS_EVENTS);
        assertThat(messagePublisher.payload).isInstanceOf(RealtimeEnvelope.class);
        RealtimeEnvelope<?> envelope = (RealtimeEnvelope<?>) messagePublisher.payload;
        assertThat(envelope.type()).isEqualTo(StompBusinessRealtimeNotifier.MESSAGE_TYPE);
        assertThat(envelope.traceId()).isEqualTo("trace-1");
        assertThat(envelope.payload()).isEqualTo(event);
    }

    @Test
    void shouldFanOutUserSessionChangesOnlyToAuthorizedOnlineManagers() {
        SimpUserRegistry registry = mock(SimpUserRegistry.class);
        PlatformRecordActionAvailabilityService actionAvailabilityService =
                mock(PlatformRecordActionAvailabilityService.class);
        UserSessionService userSessionService = mock(UserSessionService.class);
        RecordingBusinessRealtimeNotifier notifier = new RecordingBusinessRealtimeNotifier();
        UserSessionManagementRealtimeEventPublisher publisher =
                new UserSessionManagementRealtimeEventPublisher(
                        registry, actionAvailabilityService, notifier, userSessionService);
        CurrentUser admin = CurrentUser.systemUser("admin-1", "Admin");
        CurrentUser viewer = CurrentUser.tenantUser("viewer-1", "Viewer", "demo");
        CurrentUser target = CurrentUser.tenantUser("user-1", "User", "demo");
        SimpUser adminUser = simpUser(admin);
        SimpUser viewerUser = simpUser(viewer);
        SimpUser targetUser = simpUser(target);
        when(registry.getUsers()).thenReturn(Set.of(adminUser, viewerUser, targetUser));
        when(userSessionService.currentUser("token-admin-1")).thenReturn(Optional.of(admin));
        when(userSessionService.currentUser("token-viewer-1")).thenReturn(Optional.of(viewer));
        when(userSessionService.currentUser("token-user-1")).thenReturn(Optional.of(target));
        when(actionAvailabilityService.recordActions("iam.user", "user-1")).thenAnswer(invocation -> {
            String currentUserId = CurrentUserContext.currentUser()
                    .map(CurrentUser::userId)
                    .orElse(null);
            if ("admin-1".equals(currentUserId)) {
                return new PlatformRecordActionAvailability("user-1",
                        List.of(new PlatformRecordActionAvailability.Action("sessions", true, null)));
            }
            return new PlatformRecordActionAvailability("user-1",
                    List.of(new PlatformRecordActionAvailability.Action("sessions", false, "no data auth")));
        });

        publisher.publish(UserSessionLifecycleEvent.loggedIn("user-1", "session-1"));

        assertThat(notifier.userIds).containsExactly("admin-1");
        assertThat(notifier.events).containsExactly(BusinessRealtimeEvent.userSessionChanged("user-1", "LOGGED_IN"));
    }

    @Test
    void shouldSkipUserSessionChangeFanOutWhenManagerSessionIsNoLongerValid() {
        SimpUserRegistry registry = mock(SimpUserRegistry.class);
        PlatformRecordActionAvailabilityService actionAvailabilityService =
                mock(PlatformRecordActionAvailabilityService.class);
        UserSessionService userSessionService = mock(UserSessionService.class);
        RecordingBusinessRealtimeNotifier notifier = new RecordingBusinessRealtimeNotifier();
        UserSessionManagementRealtimeEventPublisher publisher =
                new UserSessionManagementRealtimeEventPublisher(
                        registry, actionAvailabilityService, notifier, userSessionService);
        CurrentUser admin = CurrentUser.systemUser("admin-1", "Admin");
        SimpUser adminUser = simpUser(admin);
        when(registry.getUsers()).thenReturn(Set.of(adminUser));
        when(userSessionService.currentUser("token-admin-1")).thenReturn(Optional.empty());

        publisher.publish(UserSessionLifecycleEvent.loggedIn("user-1", "session-1"));

        assertThat(notifier.userIds).isEmpty();
    }

    @Test
    void shouldSendForceLogoutSecurityNotificationsToUserQueue() {
        RecordingRealtimeMessagePublisher messagePublisher = new RecordingRealtimeMessagePublisher();
        StompSecurityRealtimeNotifier notifier = new StompSecurityRealtimeNotifier(messagePublisher);

        notifier.notifyForceLogout("user-1");

        assertThat(messagePublisher.userId).isEqualTo("user-1");
        assertThat(messagePublisher.queue).isEqualTo(RealtimeDestinations.USER_NOTIFICATIONS);
        assertThat(messagePublisher.payload).isInstanceOf(RealtimeEnvelope.class);
        RealtimeEnvelope<?> envelope = (RealtimeEnvelope<?>) messagePublisher.payload;
        assertThat(envelope.type()).isEqualTo(StompSecurityRealtimeNotifier.MESSAGE_TYPE);
        assertThat(envelope.payload()).isEqualTo(SecurityNotification.forceLogout());
    }

    @Test
    void shouldSendSessionRevokedSecurityNotificationsToUserQueue() {
        RecordingRealtimeMessagePublisher messagePublisher = new RecordingRealtimeMessagePublisher();
        StompSecurityRealtimeNotifier notifier = new StompSecurityRealtimeNotifier(messagePublisher);

        notifier.notifySessionRevoked("user-1", "session-1");

        assertThat(messagePublisher.userId).isEqualTo("user-1");
        assertThat(messagePublisher.queue).isEqualTo(RealtimeDestinations.USER_NOTIFICATIONS);
        assertThat(messagePublisher.payload).isInstanceOf(RealtimeEnvelope.class);
        RealtimeEnvelope<?> envelope = (RealtimeEnvelope<?>) messagePublisher.payload;
        assertThat(envelope.type()).isEqualTo(StompSecurityRealtimeNotifier.MESSAGE_TYPE);
        assertThat(envelope.payload()).isEqualTo(SecurityNotification.sessionRevoked("session-1"));
    }

    @Test
    void shouldAdaptUserSecurityEventsToSessionRevocationAndRealtimeNotifications() {
        RecordingSecurityRealtimeNotifier notifier = new RecordingSecurityRealtimeNotifier();
        UserSecurityRealtimeEventPublisher publisher = new UserSecurityRealtimeEventPublisher(notifier);

        publisher.publish(UserSecurityEvent.passwordChanged("user-1"));
        publisher.publish(UserSecurityEvent.passwordReset("user-2"));
        publisher.publish(UserSecurityEvent.forceLogout("user-3"));

        assertThat(notifier.changedUserIds).containsExactly("user-1");
        assertThat(notifier.resetUserIds).containsExactly("user-2");
        assertThat(notifier.forceLogoutUserIds).containsExactly("user-3");
    }

    @Test
    void shouldAdaptSessionRevokedEventToTargetedRealtimeNotification() {
        RecordingSecurityRealtimeNotifier notifier = new RecordingSecurityRealtimeNotifier();
        UserSecurityRealtimeEventPublisher publisher = new UserSecurityRealtimeEventPublisher(notifier);

        publisher.publish(UserSecurityEvent.sessionRevoked("user-1", "session-1"));

        assertThat(notifier.revokedSessions).containsExactly("user-1:session-1");
    }

    @Test
    void shouldRejectRealtimeSubscribeWhenBoundSessionIsRevoked() {
        UserSessionService userSessionService = mock(UserSessionService.class);
        RealtimeAuthenticationChannelInterceptor interceptor =
                new RealtimeAuthenticationChannelInterceptor(userSessionService);
        CurrentUser currentUser = CurrentUser.tenantUser("user-1", "User", "tenant-a");
        when(userSessionService.currentUser("token-1"))
                .thenReturn(Optional.of(currentUser), Optional.empty());

        Message<?> connected = interceptor.preSend(stompMessage(StompCommand.CONNECT, null, "token-1"),
                mock(MessageChannel.class));
        CurrentUserPrincipal principal = (CurrentUserPrincipal) StompHeaderAccessor.wrap(connected).getUser();

        assertThat(principal.currentUser()).isEqualTo(currentUser);
        assertThat(principal.token()).isEqualTo("token-1");
        assertThatThrownBy(() -> interceptor.preSend(stompMessage(StompCommand.SUBSCRIBE, principal, null),
                mock(MessageChannel.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("realtime authentication required");
    }

    private static final class RecordingRealtimeMessagePublisher implements RealtimeMessagePublisher {
        private RealtimeTopic topic;
        private String userId;
        private RealtimeQueue queue;
        private Object payload;
        private final List<String> users = new ArrayList<>();

        @Override
        public void broadcast(RealtimeTopic topic, Object payload) {
            this.topic = topic;
            this.payload = payload;
        }

        @Override
        public void sendToUser(String userId, RealtimeQueue queue, Object payload) {
            this.userId = userId;
            this.queue = queue;
            this.payload = payload;
            users.add(userId);
        }
    }

    private Message<?> stompMessage(StompCommand command, CurrentUserPrincipal principal, String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        if (principal != null) {
            accessor.setUser(principal);
        }
        if (token != null) {
            accessor.addNativeHeader("Authorization", "Bearer " + token);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private SimpUser simpUser(CurrentUser currentUser) {
        SimpUser user = mock(SimpUser.class);
        when(user.getPrincipal()).thenReturn(new CurrentUserPrincipal(currentUser, "token-" + currentUser.userId()));
        return user;
    }

    private static final class RecordingBusinessRealtimeNotifier implements BusinessRealtimeNotifier {
        private final List<String> userIds = new ArrayList<>();
        private final List<BusinessRealtimeEvent> events = new ArrayList<>();

        @Override
        public void notifyUser(String userId, BusinessRealtimeEvent event) {
            userIds.add(userId);
            events.add(event);
        }
    }

    private static final class RecordingSecurityRealtimeNotifier implements SecurityRealtimeNotifier {
        private final List<String> changedUserIds = new ArrayList<>();
        private final List<String> resetUserIds = new ArrayList<>();
        private final List<String> forceLogoutUserIds = new ArrayList<>();
        private final List<String> revokedSessions = new ArrayList<>();

        @Override
        public void notifyPasswordChanged(String userId) {
            changedUserIds.add(userId);
        }

        @Override
        public void notifyPasswordReset(String userId) {
            resetUserIds.add(userId);
        }

        @Override
        public void notifyForceLogout(String userId) {
            forceLogoutUserIds.add(userId);
        }

        @Override
        public void notifySessionRevoked(String userId, String sessionId) {
            revokedSessions.add(userId + ":" + sessionId);
        }
    }
}
