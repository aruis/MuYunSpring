package net.ximatai.muyun.spring.boot.platform;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;

import java.util.Comparator;
import java.util.List;

public class PlatformBootstrapRunner {
    private final List<PlatformBootstrapTask> tasks;

    public PlatformBootstrapRunner(List<PlatformBootstrapTask> tasks) {
        this.tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }

    void onStart(@Observes StartupEvent event) {
        tasks.stream()
                .sorted(Comparator.comparingInt(PlatformBootstrapTask::order)
                        .thenComparing(PlatformBootstrapTask::name))
                .forEach(PlatformBootstrapTask::run);
    }
}
