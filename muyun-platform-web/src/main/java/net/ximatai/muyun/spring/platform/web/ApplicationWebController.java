package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.platform.module.PlatformStaticModule;

import net.ximatai.muyun.spring.web.SystemScope;
import net.ximatai.muyun.spring.web.WebSupport;
import net.ximatai.muyun.spring.platform.application.Application;
import net.ximatai.muyun.spring.platform.application.ApplicationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.platform.application.PlatformApplication.class, alias = ApplicationService.MODULE_ALIAS, title = "平台应用")
@StaticModuleOpenApi
@PlatformMenu(parent = PlatformMenuGroups.MODELING, title = "应用管理", order = 10)
@RequestMapping("/platform.application")
public class ApplicationWebController extends WebSupport<ApplicationService> implements
        CrudWeb<Application, ApplicationService>,
        SystemScope<ApplicationService> {
}
