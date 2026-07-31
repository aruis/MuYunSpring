package net.ximatai.muyun.spring.boot.demo.school.student;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

/**
 * 学生的标准 Web 交付入口；{@link CrudWeb} 提供 CRUD 与表单/查询 schema。
 * 启停和回收站由 Service Ability 自动投射，不在 Controller 重复实现。
 */
@RestController
@Profile("school-demo")
@PlatformStaticModule(application = "education", alias = "education.student", title = "学生")
@RequestMapping("/education.student")
public class StudentWebController extends WebSupport<StudentService>
        implements CrudWeb<Student, StudentService> {
}
