package net.ximatai.muyun.spring.boot.demo.school.classroom;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

@RestController
@Profile("school-demo")
@PlatformStaticModule(application = "education", alias = "education.classroom", title = "班级")
@RequestMapping("/education.classroom")
public class ClassroomWebController extends WebSupport<ClassroomService>
        implements CrudWeb<Classroom, ClassroomService> {
    // 成员是班级聚合的子表，随班级新增/更新请求中的 members 一起保存，
    // 因而不暴露脱离班级生命周期的 ClassMemberController。
    // 班级服务声明的排序和回收站 Ability 端点由平台自动投射。
}
