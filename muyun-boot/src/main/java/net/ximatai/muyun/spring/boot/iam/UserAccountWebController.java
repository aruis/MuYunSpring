package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.iam.user.UserAccount;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PlatformStaticModule(application = "iam", alias = "iam.user", title = "用户管理")
@RequestMapping("/iam.user")
public class UserAccountWebController extends WebSupport<UserAccountService> implements
        CrudWeb<UserAccount, UserAccountService>,
        EnableWeb<UserAccount, UserAccountService>,
        SortWeb<UserAccount, UserAccountService> {
    private final UserSessionService userSessionService;

    public UserAccountWebController(ObjectProvider<UserSessionService> userSessionService) {
        this.userSessionService = userSessionService == null ? null : userSessionService.getIfAvailable();
    }

    @PostMapping("/changePassword/{id}")
    @CustomActionEndpoint(value = "changePassword", title = "修改密码",
            level = PlatformActionLevel.RECORD, dataAuth = true)
    public WebCountResponse changePassword(@PathVariable String id,
                                           @RequestBody ChangePasswordRequest request) {
        return webScope(() -> {
            int changed = service().changePassword(id, request.password());
            if (changed > 0 && userSessionService != null) {
                userSessionService.revokeUserSessions(id);
            }
            return new WebCountResponse(changed);
        });
    }

    public record ChangePasswordRequest(String password) {
    }
}
