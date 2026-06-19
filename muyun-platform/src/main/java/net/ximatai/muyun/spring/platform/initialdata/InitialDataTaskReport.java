package net.ximatai.muyun.spring.platform.initialdata;

import java.util.List;

public record InitialDataTaskReport(
        String name,
        int order,
        List<InitialDataResult> results
) {
    public InitialDataTaskReport {
        results = results == null ? List.of() : List.copyOf(results);
    }
}
