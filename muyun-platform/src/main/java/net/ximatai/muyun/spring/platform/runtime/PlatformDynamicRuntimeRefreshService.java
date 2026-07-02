package net.ximatai.muyun.spring.platform.runtime;

import net.ximatai.muyun.spring.dynamic.refresh.DynamicModuleRefreshResult;
import jakarta.enterprise.context.Dependent;

import java.util.Objects;

@Dependent
public class PlatformDynamicRuntimeRefreshService {
    private final PlatformDynamicRuntimeRefresher refresher;

    public PlatformDynamicRuntimeRefreshService(PlatformDynamicRuntimeRefresher refresher) {
        this.refresher = Objects.requireNonNull(refresher, "refresher must not be null");
    }

    public DynamicModuleRefreshResult refresh(String moduleAlias) {
        return refresher.refresh(moduleAlias);
    }

    public DynamicModuleRefreshResult executeRefresh(String moduleAlias) {
        return refresher.executeRefresh(moduleAlias);
    }

    public DynamicModuleRefreshResult previewRefresh(String moduleAlias) {
        return refresher.previewRefresh(moduleAlias);
    }
}
