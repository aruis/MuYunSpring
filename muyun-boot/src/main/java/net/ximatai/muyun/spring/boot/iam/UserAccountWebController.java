package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.iam.user.UserAccount;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.iam.user.UserAccountView;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/iam.user")
public class UserAccountWebController {
    private final UserAccountService userAccountService;

    public UserAccountWebController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @PostMapping("/create")
    public UserAccountView create(@RequestBody CreateUserRequest request) {
        UserAccount user = new UserAccount();
        user.setUsername(request.username());
        user.setTitle(request.title());
        user.setMobile(request.mobile());
        user.setEmail(request.email());
        user.setOrganizationId(request.organizationId());
        String id = userAccountService.createUser(user, request.password());
        return UserAccountView.of(userAccountService.select(id));
    }

    @PostMapping("/password/{userId}")
    public WebCountResponse changePassword(@PathVariable String userId,
                                           @RequestBody ChangePasswordRequest request) {
        return new WebCountResponse(userAccountService.changePassword(userId, request.password()));
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
