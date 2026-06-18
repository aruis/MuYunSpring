package net.ximatai.muyun.spring.platform.initialdata;

import java.util.List;

public record InitialDataExecutionReport(List<InitialDataContributionReport> contributions) {
    public InitialDataExecutionReport {
        contributions = contributions == null ? List.of() : List.copyOf(contributions);
    }

    public List<InitialDataResult> results() {
        return contributions.stream()
                .flatMap(contribution -> contribution.results().stream())
                .toList();
    }
}
