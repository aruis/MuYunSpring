package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.platform.runtime.PlatformBootstrapTask;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformBootstrapRunnerTest {
    @Test
    void shouldRunPlatformBootstrapTasksByOrderThenName() throws Exception {
        List<String> executed = new ArrayList<>();
        PlatformBootstrapRunner runner = new PlatformBootstrapRunner(List.of(
                task("metadata", 20, executed),
                task("applications", -10, executed),
                task("modules", 20, executed)
        ));

        runner.run(null);

        assertThat(executed).containsExactly("applications", "metadata", "modules");
    }

    private PlatformBootstrapTask task(String name, int order, List<String> executed) {
        return new PlatformBootstrapTask() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public int order() {
                return order;
            }

            @Override
            public void run() {
                executed.add(name);
            }
        };
    }
}
