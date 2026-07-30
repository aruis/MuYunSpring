package net.ximatai.muyun.spring.boot.demo.school.classroom;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

/**
 * 班级的标准 Web 交付入口。
 * 成员随班级请求中的 {@code members} 保存，故不暴露脱离聚合生命周期的成员 Controller；
 * 排序与回收站端点由 Service Ability 自动投射。
 */
@RestController
@Profile("school-demo")
@PlatformStaticModule(application = "education", alias = "education.classroom", title = "班级")
@RequestMapping("/education.classroom")
public class ClassroomWebController extends WebSupport<ClassroomService>
        implements CrudWeb<Classroom, ClassroomService> {
}
