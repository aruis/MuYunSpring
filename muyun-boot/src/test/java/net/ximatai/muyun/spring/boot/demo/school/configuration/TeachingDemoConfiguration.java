package net.ximatai.muyun.spring.boot.demo.school.configuration;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticApplication;
import net.ximatai.muyun.spring.boot.demo.school.classroom.ClassMemberDao;
import net.ximatai.muyun.spring.boot.demo.school.classroom.ClassMemberService;
import net.ximatai.muyun.spring.boot.demo.school.classroom.ClassroomDao;
import net.ximatai.muyun.spring.boot.demo.school.classroom.ClassroomService;
import net.ximatai.muyun.spring.boot.demo.school.classroom.ClassroomWebController;
import net.ximatai.muyun.spring.boot.demo.school.hobby.HobbyDao;
import net.ximatai.muyun.spring.boot.demo.school.hobby.HobbyService;
import net.ximatai.muyun.spring.boot.demo.school.hobby.HobbyWebController;
import net.ximatai.muyun.spring.boot.demo.school.student.StudentDao;
import net.ximatai.muyun.spring.boot.demo.school.student.StudentService;
import net.ximatai.muyun.spring.boot.demo.school.student.StudentWebController;
import net.ximatai.muyun.spring.boot.demo.school.teacher.TeacherDao;
import net.ximatai.muyun.spring.boot.demo.school.teacher.TeacherService;
import net.ximatai.muyun.spring.boot.demo.school.teacher.TeacherWebController;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryInitialDataDeclarations;

/** 仅测试使用的装配入口，模拟静态业务模块接入最终 Boot 运行时的方式。 */
@TestConfiguration(proxyBeanMethods = false)
@PlatformStaticApplication(alias = "education", title = "教学管理", sortOrder = 100)
@Profile("school-demo")
public class TeachingDemoConfiguration {
    @Bean
    TeachingDictionaryInitialDataProvider teachingDictionaryInitialDataProvider(
            DictionaryInitialDataDeclarations dictionaries) {
        return new TeachingDictionaryInitialDataProvider(dictionaries);
    }

    @Bean
    HobbyService hobbyService(HobbyDao dao) {
        return new HobbyService(dao);
    }

    @Bean
    StudentService studentService(StudentDao dao, HobbyService hobbyService) {
        return new StudentService(dao, hobbyService);
    }

    @Bean
    TeacherService teacherService(TeacherDao dao) {
        return new TeacherService(dao);
    }

    @Bean
    ClassMemberService classMemberService(ClassMemberDao dao, StudentService studentService) {
        return new ClassMemberService(dao, studentService);
    }

    @Bean
    ClassroomService classroomService(ClassroomDao dao,
                                      TeacherService teacherService,
                                      ClassMemberService memberService) {
        return new ClassroomService(dao, teacherService, memberService);
    }

    @Bean
    StudentWebController studentWebController() {
        return new StudentWebController();
    }

    @Bean
    TeacherWebController teacherWebController() {
        return new TeacherWebController();
    }

    @Bean
    ClassroomWebController classroomWebController() {
        return new ClassroomWebController();
    }

    @Bean
    HobbyWebController hobbyWebController() {
        return new HobbyWebController();
    }
}
