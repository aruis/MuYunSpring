package net.ximatai.muyun.spring.platform.ui;

import java.util.List;

public record PlatformRecordNavigationContext(
        String sessionId,
        String moduleAlias,
        String entityAlias,
        List<String> recordIds,
        int pageNum,
        int pageSize,
        long total
) {
}
