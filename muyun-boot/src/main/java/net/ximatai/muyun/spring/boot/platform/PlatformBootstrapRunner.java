package net.ximatai.muyun.spring.boot.platform;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;

import java.util.Comparator;
import java.util.List;

public class PlatformBootstrapRunner implements ApplicationRunner, Ordered {
    private final List<PlatformBootstrapTask> tasks;

    public PlatformBootstrapRunner(List<PlatformBootstrapTask> tasks) {
        this.tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }

    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public void run(ApplicationArguments args) {
        tasks.stream()
                .sorted(Comparator.comparingInt(PlatformBootstrapTask::order)
                        .thenComparing(PlatformBootstrapTask::name))
                .forEach(PlatformBootstrapTask::run);
    }
}
