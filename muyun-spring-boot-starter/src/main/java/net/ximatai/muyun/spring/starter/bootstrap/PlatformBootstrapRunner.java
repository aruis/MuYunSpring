package net.ximatai.muyun.spring.starter.bootstrap;

import net.ximatai.muyun.spring.platform.runtime.PlatformBootstrapTask;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;

import java.util.Comparator;
import java.util.List;

/**
 * Spring 应用启动后的统一平台任务执行器。
 * 任务排序不依赖 Spring Bean 注册顺序，以保证不同装配组合下的启动结果一致。
 */
public class PlatformBootstrapRunner implements ApplicationRunner, Ordered {
    private final List<PlatformBootstrapTask> tasks;

    public PlatformBootstrapRunner(List<PlatformBootstrapTask> tasks) {
        this.tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }

    @Override
    /** 在普通 ApplicationRunner 之前执行平台托管数据与声明同步。 */
    public int getOrder() {
        return 0;
    }

    @Override
    /** 先按显式顺序、再按稳定名称执行，避免同优先级任务出现非确定行为。 */
    public void run(ApplicationArguments args) {
        tasks.stream()
                .sorted(Comparator.comparingInt(PlatformBootstrapTask::order)
                        .thenComparing(PlatformBootstrapTask::name))
                .forEach(PlatformBootstrapTask::run);
    }
}
