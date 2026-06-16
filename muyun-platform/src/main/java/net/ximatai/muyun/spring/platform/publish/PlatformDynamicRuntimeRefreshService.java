package net.ximatai.muyun.spring.platform.publish;

import net.ximatai.muyun.spring.dynamic.publish.DynamicModulePublishResult;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class PlatformDynamicRuntimeRefreshService {
    private final PlatformDynamicModulePublisher publisher;

    public PlatformDynamicRuntimeRefreshService(PlatformDynamicModulePublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
    }

    public DynamicModulePublishResult refresh(String moduleAlias) {
        return publisher.publish(moduleAlias);
    }

    public DynamicModulePublishResult previewRefresh(String moduleAlias) {
        return publisher.preview(moduleAlias);
    }
}
