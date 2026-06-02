package net.ximatai.muyun.spring.ability.event;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class RuntimeEventMulticaster implements RuntimeEventPublisher {
    private final Supplier<List<RuntimeEventListener>> listenerSupplier;

    public RuntimeEventMulticaster(List<RuntimeEventListener> listeners) {
        this(() -> copyListeners(listeners));
    }

    public RuntimeEventMulticaster(Supplier<List<RuntimeEventListener>> listenerSupplier) {
        this.listenerSupplier = listenerSupplier == null ? List::of : listenerSupplier;
    }

    @Override
    public void publish(RuntimeEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        for (RuntimeEventListener listener : listeners()) {
            listener.onRuntimeEvent(event);
        }
    }

    public List<RuntimeEventListener> listeners() {
        return copyListeners(listenerSupplier.get());
    }

    public static RuntimeEventPublisher of(List<RuntimeEventListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return RuntimeEventPublisher.noop();
        }
        return new RuntimeEventMulticaster(listeners);
    }

    private static List<RuntimeEventListener> copyListeners(List<RuntimeEventListener> listeners) {
        return listeners == null ? List.of() : List.copyOf(listeners);
    }
}
