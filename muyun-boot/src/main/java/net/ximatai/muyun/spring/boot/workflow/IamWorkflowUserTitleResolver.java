package net.ximatai.muyun.spring.boot.workflow;

import net.ximatai.muyun.spring.iam.user.UserAccount;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowUserTitleResolver;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class IamWorkflowUserTitleResolver implements WorkflowUserTitleResolver {
    private final UserAccountService userAccountService;

    public IamWorkflowUserTitleResolver(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @Override
    public Map<String, String> titles(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<String, String> titles = new LinkedHashMap<>();
        for (String userId : userIds) {
            if (userId == null || userId.isBlank() || titles.containsKey(userId)) {
                continue;
            }
            UserAccount user = userAccountService.select(userId);
            if (user != null && user.getTitle() != null && !user.getTitle().isBlank()) {
                titles.put(userId, user.getTitle());
            }
        }
        return Map.copyOf(titles);
    }
}
