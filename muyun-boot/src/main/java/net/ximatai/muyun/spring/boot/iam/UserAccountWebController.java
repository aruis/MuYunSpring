package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.iam.user.UserAccount;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.iam.user.UserAccountView;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/iam.user")
public class UserAccountWebController extends WebSupport<UserAccountService> {
    private final UserSessionService userSessionService;

    public UserAccountWebController(ObjectProvider<UserSessionService> userSessionService) {
        this.userSessionService = userSessionService == null ? null : userSessionService.getIfAvailable();
    }

    @PostMapping("/create")
    @ActionEndpoint(PlatformAction.CREATE)
    public UserAccountView create(@RequestBody CreateUserRequest request) {
        return webScope(() -> {
            UserAccount user = new UserAccount();
            user.setUsername(request.username());
            user.setTitle(request.title());
            user.setMobile(request.mobile());
            user.setEmail(request.email());
            user.setOrganizationId(request.organizationId());
            String id = service().createUser(user, request.password());
            return UserAccountView.of(service().select(id));
        });
    }

    @PostMapping("/password/{userId}")
    @ActionEndpoint(PlatformAction.UPDATE)
    public WebCountResponse changePassword(@PathVariable String userId,
                                           @RequestBody ChangePasswordRequest request) {
        return webScope(() -> {
            int changed = service().changePassword(userId, request.password());
            if (changed > 0 && userSessionService != null) {
                userSessionService.revokeUserSessions(userId);
            }
            return new WebCountResponse(changed);
        });
    }

    public record CreateUserRequest(
            String username,
            String title,
            String mobile,
            String email,
            String organizationId,
            String password
    ) {
    }

    public record ChangePasswordRequest(String password) {
    }
}
