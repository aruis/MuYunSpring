package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.event.RuntimeEvent;
import net.ximatai.muyun.spring.ability.event.RuntimeEventListener;
import net.ximatai.muyun.spring.ability.event.RuntimeEventMulticaster;
import net.ximatai.muyun.spring.ability.event.RuntimeEventPublisher;
import net.ximatai.muyun.spring.ability.event.RuntimeEventType;
import net.ximatai.muyun.spring.ability.event.RuntimeMutationSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuntimeEventMulticasterTest {
    @Test
    void shouldDispatchRuntimeEventToListenersInOrder() {
        List<String> received = new ArrayList<>();
        RuntimeEvent event = event();
        RuntimeEventMulticaster multicaster = new RuntimeEventMulticaster(List.of(
                receivedEvent -> received.add("first:" + receivedEvent.eventId()),
                receivedEvent -> received.add("second:" + receivedEvent.eventId())
        ));

        multicaster.publish(event);

        assertThat(received).containsExactly("first:" + event.eventId(), "second:" + event.eventId());
    }

    @Test
    void shouldFailFastWhenListenerFails() {
        List<String> received = new ArrayList<>();
        RuntimeEventListener failing = event -> {
            throw new IllegalStateException("listener failed");
        };
        RuntimeEventMulticaster multicaster = new RuntimeEventMulticaster(List.of(
                event -> received.add("first"),
                failing,
                event -> received.add("third")
        ));

        assertThatThrownBy(() -> multicaster.publish(event()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("listener failed");
        assertThat(received).containsExactly("first");
    }

    @Test
    void shouldReturnNoopPublisherWhenNoListenerExists() {
        RuntimeEventPublisher publisher = RuntimeEventMulticaster.of(List.of());

        publisher.publish(event());

        assertThat(publisher).isSameAs(RuntimeEventPublisher.noop());
    }

    private RuntimeEvent event() {
        return RuntimeEvent.of(RuntimeEventType.AFTER_CREATE, "sales.contract", "contract", "contract-1",
                null, "tenant-1", false, RuntimeMutationSource.BUSINESS, java.util.Map.of());
    }
}
