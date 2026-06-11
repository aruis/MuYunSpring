package net.ximatai.muyun.spring.platform.config;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LowCodeModuleHealthService {
    private final List<LowCodeModuleHealthChecker> checkers;

    public LowCodeModuleHealthService(List<LowCodeModuleHealthChecker> checkers) {
        this.checkers = checkers == null
                ? List.of()
                : checkers.stream()
                .filter(checker -> checker != null)
                .toList();
    }

    public LowCodeConfigHealthReport check(LowCodeModuleHealthContext context) {
        if (context == null) {
            throw new IllegalArgumentException("health context must not be null");
        }
        List<LowCodeConfigHealthItem> items = new ArrayList<>();
        for (LowCodeModuleHealthChecker checker : checkers) {
            List<LowCodeConfigHealthItem> checked = checker.check(context);
            if (checked != null) {
                items.addAll(checked);
            }
        }
        return LowCodeConfigHealthReport.of(context.moduleAlias(), items);
    }
}
