package net.ximatai.muyun.spring.boot.demo.school.hobby;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

/** 爱好分类树的标准 Web 交付入口；树、启停与引用候选均由 Service Ability 统一提供。 */
@RestController
@Profile("school-demo")
@PlatformStaticModule(application = "education", alias = "education.hobby", title = "爱好")
@RequestMapping("/education.hobby")
public class HobbyWebController extends WebSupport<HobbyService>
        implements CrudWeb<Hobby, HobbyService> {
}
