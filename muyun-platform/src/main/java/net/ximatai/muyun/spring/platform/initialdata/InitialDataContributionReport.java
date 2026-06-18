package net.ximatai.muyun.spring.platform.initialdata;

import java.util.List;

public record InitialDataContributionReport(
        String name,
        int order,
        List<InitialDataResult> results
) {
    public InitialDataContributionReport {
        results = results == null ? List.of() : List.copyOf(results);
    }
}
