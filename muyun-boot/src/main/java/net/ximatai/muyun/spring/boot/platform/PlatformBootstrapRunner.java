package net.ximatai.muyun.spring.boot.platform;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;

import java.util.Comparator;
import java.util.List;

public class PlatformBootstrapRunner {
    private final List<PlatformBootstrapTask> tasks;
    private final boolean enabled;

    public PlatformBootstrapRunner(List<PlatformBootstrapTask> tasks) {
        this(tasks, true);
    }

    public PlatformBootstrapRunner(List<PlatformBootstrapTask> tasks, boolean enabled) {
        this.tasks = tasks == null ? List.of() : List.copyOf(tasks);
        this.enabled = enabled;
    }

    void onStart(@Observes StartupEvent event) {
        if (!enabled) {
            return;
        }
        tasks.stream()
                .sorted(Comparator.comparingInt(PlatformBootstrapTask::order)
                        .thenComparing(PlatformBootstrapTask::name))
                .forEach(PlatformBootstrapTask::run);
    }
}
