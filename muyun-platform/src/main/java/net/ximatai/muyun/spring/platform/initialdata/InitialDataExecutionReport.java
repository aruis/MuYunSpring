package net.ximatai.muyun.spring.platform.initialdata;

import java.util.List;

public record InitialDataExecutionReport(List<InitialDataTaskReport> tasks) {
    public InitialDataExecutionReport {
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }

    public List<InitialDataResult> results() {
        return tasks.stream()
                .flatMap(task -> task.results().stream())
                .toList();
    }
}
