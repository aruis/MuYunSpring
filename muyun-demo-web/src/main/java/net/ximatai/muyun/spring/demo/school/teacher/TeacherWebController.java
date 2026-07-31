package net.ximatai.muyun.spring.demo.school.teacher;

import net.ximatai.muyun.spring.demo.school.configuration.EducationApplication;
import net.ximatai.muyun.spring.platform.web.PlatformStaticModule;
import net.ximatai.muyun.spring.platform.web.CrudWeb;
import net.ximatai.muyun.spring.web.WebSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

/** 教师的标准 Web 交付入口；教学学科候选项由字段字典声明统一交付，无需专用枚举接口。 */
@RestController
@Profile("school-demo")
@PlatformStaticModule(application = EducationApplication.class, alias = TeacherService.MODULE_ALIAS, title = "教师")
@RequestMapping("/" + TeacherService.MODULE_ALIAS)
public class TeacherWebController extends WebSupport<TeacherService>
        implements CrudWeb<Teacher, TeacherService> {
}
