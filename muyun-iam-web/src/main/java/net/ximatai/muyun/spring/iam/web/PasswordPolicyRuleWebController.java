package net.ximatai.muyun.spring.iam.web;

import net.ximatai.muyun.spring.platform.web.PlatformMenu;
import net.ximatai.muyun.spring.platform.web.PlatformMenuGroups;
import net.ximatai.muyun.spring.platform.module.PlatformStaticModule;
import net.ximatai.muyun.spring.platform.web.CrudWeb;
import net.ximatai.muyun.spring.web.SystemScope;
import net.ximatai.muyun.spring.web.WebSupport;
import net.ximatai.muyun.spring.iam.user.PasswordPolicyRule;
import net.ximatai.muyun.spring.iam.user.PasswordPolicyRuleService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.iam.application.IamApplication.class, alias = PasswordPolicyRuleService.MODULE_ALIAS, title = "密码策略规则",
        route = "/platform/security/passwords")
@PlatformMenu(parent = PlatformMenuGroups.SECURITY_AUDIT, title = "密码管理", order = 10)
@RequestMapping("/iam.password_policy_rule")
public class PasswordPolicyRuleWebController extends WebSupport<PasswordPolicyRuleService> implements
        CrudWeb<PasswordPolicyRule, PasswordPolicyRuleService>,
        SystemScope<PasswordPolicyRuleService> {
}
