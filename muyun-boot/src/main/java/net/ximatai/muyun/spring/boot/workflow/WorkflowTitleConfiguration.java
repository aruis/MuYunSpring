package net.ximatai.muyun.spring.boot.workflow;

import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowUserTitleResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class WorkflowTitleConfiguration {
    @Bean
    @ConditionalOnBean(UserAccountService.class)
    @ConditionalOnMissingBean(WorkflowUserTitleResolver.class)
    WorkflowUserTitleResolver workflowUserTitleResolver(UserAccountService userAccountService) {
        return new IamWorkflowUserTitleResolver(userAccountService);
    }
}
