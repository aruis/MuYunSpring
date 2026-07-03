package net.ximatai.muyun.spring.boot.workflow;

import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowUserTitleResolver;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class WorkflowTitleConfiguration {
    @Produces
    @ApplicationScoped
    @DefaultBean
    WorkflowUserTitleResolver workflowUserTitleResolver(UserAccountService userAccountService) {
        return new IamWorkflowUserTitleResolver(userAccountService);
    }
}
