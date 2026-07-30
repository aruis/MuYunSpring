package net.ximatai.muyun.spring.boot.demo.school.teacher;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

/** 教师的标准 Web 交付入口；教学学科候选项由字段字典声明统一交付，无需专用枚举接口。 */
@RestController
@Profile("school-demo")
@PlatformStaticModule(application = "education", alias = "education.teacher", title = "教师")
@RequestMapping("/education.teacher")
public class TeacherWebController extends WebSupport<TeacherService>
        implements CrudWeb<Teacher, TeacherService> {
}
