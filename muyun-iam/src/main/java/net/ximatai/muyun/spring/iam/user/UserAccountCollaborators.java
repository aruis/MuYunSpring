package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.spring.common.platform.AllowAllDataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.iam.role.AccountRoleGrantDao;

import java.util.function.Supplier;

public record UserAccountCollaborators(
        Supplier<DataScopeCriteriaService> dataScopeCriteriaService,
        PasswordPolicyRuleService passwordPolicyRuleService,
        AccountRoleGrantDao accountRoleGrantDao,
        Supplier<UserSecurityEventPublisher> securityEventPublisher,
        Supplier<UserSessionRevocationService> sessionRevocationService
) {
    public UserAccountCollaborators {
        dataScopeCriteriaService = dataScopeCriteriaService == null
                ? AllowAllDataScopeCriteriaService::new
                : dataScopeCriteriaService;
        securityEventPublisher = securityEventPublisher == null
                ? () -> UserSecurityEventPublisher.NOOP
                : securityEventPublisher;
        sessionRevocationService = sessionRevocationService == null ? () -> null : sessionRevocationService;
    }

    public static UserAccountCollaborators empty() {
        return new UserAccountCollaborators(null, null, null, null, null);
    }
}
