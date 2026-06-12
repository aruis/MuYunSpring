package net.ximatai.muyun.spring.ability.event;

import java.util.Objects;
import java.util.function.Supplier;

public final class RuntimeEventHandlerListener implements RuntimeEventListener {
    private final Supplier<RuntimeEventHandlerRegistry> registrySupplier;

    public RuntimeEventHandlerListener(RuntimeEventHandlerRegistry registry) {
        this(() -> registry);
    }

    public RuntimeEventHandlerListener(Supplier<RuntimeEventHandlerRegistry> registrySupplier) {
        this.registrySupplier = registrySupplier == null
                ? () -> new RuntimeEventHandlerRegistry(java.util.List.of())
                : registrySupplier;
    }

    @Override
    public void onRuntimeEvent(RuntimeEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        registrySupplier.get().dispatch(event);
    }
}
