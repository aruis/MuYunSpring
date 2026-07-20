package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.iam.role.AccountRoleGrantDao;

import java.util.Objects;
import java.util.function.Supplier;

public record UserAccountAuthorizationServices(
        Supplier<DataScopeCriteriaService> dataScopeCriteriaService,
        AccountRoleGrantDao accountRoleGrantDao
) {
    public UserAccountAuthorizationServices {
        Objects.requireNonNull(dataScopeCriteriaService, "dataScopeCriteriaService");
        Objects.requireNonNull(accountRoleGrantDao, "accountRoleGrantDao");
    }
}
