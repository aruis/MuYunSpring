package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.iam.user.PasswordPolicyRule;
import net.ximatai.muyun.spring.iam.user.PasswordPolicyRuleService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PlatformStaticModule(application = "iam", alias = PasswordPolicyRuleService.MODULE_ALIAS, title = "密码策略规则",
        route = "/platform/security/passwords")
@PlatformMenu(parent = PlatformMenuGroups.SECURITY_AUDIT, title = "密码管理", order = 10)
@RequestMapping("/iam.password_policy_rule")
public class PasswordPolicyRuleWebController extends WebSupport<PasswordPolicyRuleService> implements
        CrudWeb<PasswordPolicyRule, PasswordPolicyRuleService>,
        SystemScope<PasswordPolicyRuleService> {
}
