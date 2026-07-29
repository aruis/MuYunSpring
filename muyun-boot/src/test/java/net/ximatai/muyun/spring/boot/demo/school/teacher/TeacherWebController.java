package net.ximatai.muyun.spring.boot.demo.school.teacher;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

@RestController
@Profile("school-demo")
@PlatformStaticModule(application = "education", alias = "education.teacher", title = "教师")
@RequestMapping("/education.teacher")
public class TeacherWebController extends WebSupport<TeacherService>
        implements CrudWeb<Teacher, TeacherService> {
    // 教学学科字段的字典选项由模型字段声明和平台字典能力统一交付，Controller 无需单独维护枚举接口。
}
