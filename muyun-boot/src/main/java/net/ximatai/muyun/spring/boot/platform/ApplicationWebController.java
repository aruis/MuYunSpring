package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.platform.application.Application;
import net.ximatai.muyun.spring.platform.application.ApplicationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PlatformStaticModule(application = "platform", alias = ApplicationService.MODULE_ALIAS, title = "平台应用")
@PlatformMenu(parent = PlatformMenuGroups.CONFIG, title = "应用管理", order = 10)
@RequestMapping("/platform.application")
public class ApplicationWebController extends WebSupport<ApplicationService> implements
        CrudWeb<Application, ApplicationService>,
        EnableWeb<Application, ApplicationService>,
        SortWeb<Application, ApplicationService>,
        SystemScope<ApplicationService> {
}
