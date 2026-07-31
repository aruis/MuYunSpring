package net.ximatai.muyun.spring.iam.employee;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.iam.application.IamApplication;
import net.ximatai.muyun.spring.platform.module.PlatformStaticModule;
import net.ximatai.muyun.spring.platform.module.StaticModuleServiceDeclaration;
import org.springframework.stereotype.Component;

/** 声明职员账号绑定是静态 IAM 模块，但它不对外提供独立 HTTP 入口。 */
@Component
@PlatformStaticModule(
        application = IamApplication.class,
        alias = EmployeeAccountService.MODULE_ALIAS,
        title = "职员账号绑定"
)
public class EmployeeAccountStaticModule implements StaticModuleServiceDeclaration {
    private final EmployeeAccountService employeeAccountService;

    public EmployeeAccountStaticModule(EmployeeAccountService employeeAccountService) {
        this.employeeAccountService = employeeAccountService;
    }

    @Override
    public CrudAbility<?> staticModuleService() {
        return employeeAccountService;
    }
}
