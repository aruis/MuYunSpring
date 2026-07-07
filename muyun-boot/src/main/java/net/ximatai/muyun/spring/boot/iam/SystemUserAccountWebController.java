package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PlatformStaticModule(application = "iam", alias = "iam.system_user", title = "系统账号管理",
        route = "/iam/system-users")
@PlatformMenu(parent = PlatformMenuGroups.IDENTITY, order = 65)
public class SystemUserAccountWebController {
}
