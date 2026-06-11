package net.ximatai.muyun.spring.platform.config;

import java.util.List;

public interface LowCodeModuleHealthChecker {
    List<LowCodeConfigHealthItem> check(LowCodeModuleHealthContext context);
}
