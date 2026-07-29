package net.ximatai.muyun.spring.boot.demo.school.student;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Profile;

@RestController
@Profile("school-demo")
@PlatformStaticModule(application = "education", alias = "education.student", title = "学生")
@RequestMapping("/education.student")
public class StudentWebController extends WebSupport<StudentService>
        implements CrudWeb<Student, StudentService> {
    // CrudWeb 提供查询、详情、新增、更新、删除及表单/查询 schema 的标准 HTTP 契约。
    // 学生服务声明的启停、回收站等 Ability 端点由平台自动投射，不在 Controller 重复实现。
}
