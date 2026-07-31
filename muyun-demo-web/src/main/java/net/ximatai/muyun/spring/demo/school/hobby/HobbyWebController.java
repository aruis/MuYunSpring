package net.ximatai.muyun.spring.demo.school.hobby;

import net.ximatai.muyun.spring.demo.school.configuration.EducationApplication;
import net.ximatai.muyun.spring.platform.web.PlatformStaticModule;
import net.ximatai.muyun.spring.platform.web.CrudWeb;
import net.ximatai.muyun.spring.web.WebSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

/** 爱好分类树的标准 Web 交付入口；树、启停与引用候选均由 Service Ability 统一提供。 */
@RestController
@Profile("school-demo")
@PlatformStaticModule(application = EducationApplication.class, alias = HobbyService.MODULE_ALIAS, title = "爱好")
@RequestMapping("/" + HobbyService.MODULE_ALIAS)
public class HobbyWebController extends WebSupport<HobbyService>
        implements CrudWeb<Hobby, HobbyService> {
}
