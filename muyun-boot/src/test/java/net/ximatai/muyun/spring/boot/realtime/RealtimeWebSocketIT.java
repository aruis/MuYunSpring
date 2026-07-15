package net.ximatai.muyun.spring.boot.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import net.ximatai.muyun.spring.ability.action.CommittedChangeSet;
import net.ximatai.muyun.spring.ability.action.DataChange;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = RealtimeWebSocketIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class RealtimeWebSocketIT {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @LocalServerPort
    private int port;

    @Autowired
    private DataChangeRealtimePublisher dataChangeRealtimePublisher;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private SimpUserRegistry userRegistry;

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration",
            "org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration",
            "net.ximatai.muyun.database.spring.boot.MuYunDatabaseAutoConfiguration"
    })
    @Import(MuYunSpringRealtimeConfiguration.class)
    static class TestApplication {
        @Bean
        UserSessionService userSessionService() {
            return mock(UserSessionService.class);
        }
    }

    @Test
    @SuppressWarnings("removal")
    void shouldDeliverDataChangeEnvelopeOnlyToCurrentUserQueue() throws Exception {
        when(userSessionService.currentUser("token-1"))
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User 1", "tenant-a")));
        when(userSessionService.currentUser("token-2"))
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-2", "User 2", "tenant-b")));
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        BlockingQueue<JsonNode> user1Messages = new LinkedBlockingQueue<>();
        BlockingQueue<JsonNode> user2Messages = new LinkedBlockingQueue<>();

        StompSession user1Session = connect(stompClient, "token-1");
        StompSession user2Session = connect(stompClient, "token-2");
        try {
            user1Session.subscribe(userDataChangeDestination(), frameHandler(user1Messages));
            user2Session.subscribe(userDataChangeDestination(), frameHandler(user2Messages));
            awaitServerSubscriptions(userDataChangeDestination(), 2);

            try (CurrentUserContext.Scope ignored = CurrentUserContext.use(
                    CurrentUser.tenantUser("user-1", "User 1", "tenant-a"))) {
                dataChangeRealtimePublisher.publish(new CommittedChangeSet("change-set-1",
                        List.of(DataChange.recordUpdated("iam.employee", "employee-1"))));
            }

            JsonNode envelope = user1Messages.poll(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            assertThat(envelope).isNotNull();
            assertThat(envelope.path("type").asText()).isEqualTo(StompDataChangeRealtimePublisher.MESSAGE_TYPE);
            assertThat(envelope.path("payload").path("changeSetId").asText()).isEqualTo("change-set-1");
            assertThat(envelope.path("payload").path("changes").get(0).path("moduleAlias").asText())
                    .isEqualTo("iam.employee");
            assertThat(user2Messages.poll(300, TimeUnit.MILLISECONDS)).isNull();
        } finally {
            if (user1Session.isConnected()) {
                user1Session.disconnect();
            }
            if (user2Session.isConnected()) {
                user2Session.disconnect();
            }
            stompClient.stop();
        }
    }

    private StompSession connect(WebSocketStompClient stompClient, String token) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);
        return stompClient.connectAsync(
                        "ws://localhost:" + port + "/ws/platform",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {
                        })
                .get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }

    private StompFrameHandler frameHandler(BlockingQueue<JsonNode> messages) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return JsonNode.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.add((JsonNode) payload);
            }
        };
    }

    private String userDataChangeDestination() {
        return "/user" + RealtimeDestinations.DATA_CHANGES.destination();
    }

    private void awaitServerSubscriptions(String destination, int count) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (userRegistry.findSubscriptions(subscription -> destination.equals(subscription.getDestination())).size() >= count) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(50);
        }
        assertThat(userRegistry.findSubscriptions(subscription -> destination.equals(subscription.getDestination())))
                .hasSizeGreaterThanOrEqualTo(count);
    }
}
