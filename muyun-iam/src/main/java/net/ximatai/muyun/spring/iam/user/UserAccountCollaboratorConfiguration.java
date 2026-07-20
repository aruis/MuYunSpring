package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.spring.common.platform.AllowAllDataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.iam.role.AccountRoleGrantDao;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class UserAccountCollaboratorConfiguration {
    @Bean
    UserAccountCollaborators userAccountCollaborators(
            ObjectProvider<DataScopeCriteriaService> dataScopeCriteriaService,
            ObjectProvider<PasswordPolicyRuleService> passwordPolicyRuleService,
            ObjectProvider<AccountRoleGrantDao> accountRoleGrantDao,
            ObjectProvider<UserSecurityEventPublisher> securityEventPublisher,
            ObjectProvider<UserSessionRevocationService> sessionRevocationService) {
        return new UserAccountCollaborators(
                () -> dataScopeCriteriaService.getIfAvailable(AllowAllDataScopeCriteriaService::new),
                passwordPolicyRuleService.getIfAvailable(),
                accountRoleGrantDao.getIfAvailable(),
                () -> securityEventPublisher.getIfAvailable(() -> UserSecurityEventPublisher.NOOP),
                sessionRevocationService::getIfAvailable);
    }
}
