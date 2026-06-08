package net.ximatai.muyun.spring.platform.workflow;

import java.util.Collection;
import java.util.Map;

public interface WorkflowUserTitleResolver {
    WorkflowUserTitleResolver NONE = userIds -> Map.of();

    Map<String, String> titles(Collection<String> userIds);
}
