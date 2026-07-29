package net.ximatai.muyun.spring.boot.demo.school;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.boot.MuYunSpringApplication;
import net.ximatai.muyun.spring.boot.demo.school.classroom.ClassMember;
import net.ximatai.muyun.spring.boot.demo.school.classroom.ClassMemberService;
import net.ximatai.muyun.spring.boot.demo.school.classroom.Classroom;
import net.ximatai.muyun.spring.boot.demo.school.classroom.ClassroomService;
import net.ximatai.muyun.spring.boot.demo.school.configuration.TeachingDemoConfiguration;
import net.ximatai.muyun.spring.boot.demo.school.hobby.Hobby;
import net.ximatai.muyun.spring.boot.demo.school.hobby.HobbyService;
import net.ximatai.muyun.spring.boot.demo.school.student.Student;
import net.ximatai.muyun.spring.boot.demo.school.student.StudentService;
import net.ximatai.muyun.spring.boot.demo.school.teacher.Teacher;
import net.ximatai.muyun.spring.boot.demo.school.teacher.TeacherService;
import net.ximatai.muyun.spring.boot.web.endpoint.RegisteredWebEndpointCatalog;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.ability.output.PlatformRecordOutput;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionQuery;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.application.ApplicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 静态业务模块的最终交付演示：在真实 Boot 上下文中验证 Spring 装配、Repository 持久化、
 * 自动建表、静态模块/端点注册和业务 Ability 组合。
 */
@Testcontainers
@SpringBootTest(classes = MuYunSpringApplication.class)
@ContextConfiguration(classes = TeachingDemoConfiguration.class)
@ActiveProfiles("school-demo")
class TeachingDemoIT {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private StudentService students;

    @Autowired
    private HobbyService hobbies;

    @Autowired
    private TeacherService teachers;

    @Autowired
    private ClassMemberService members;

    @Autowired
    private ClassroomService classrooms;

    @Autowired
    private RegisteredWebEndpointCatalog endpointCatalog;

    @Autowired
    private OptionSourceRegistry optionSources;

    @Autowired
    private PlatformRecordOutput recordOutput;

    @Autowired
    private ApplicationService applicationService;

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("muyun.database.repository-schema-mode", () -> "ENSURE");
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void shouldRegisterDeliveredSchoolApplicationModulesAndTheirAbilityEndpoints() {
        assertThat(applicationService.select("education")).satisfies(application -> {
            assertThat(application.getTitle()).isEqualTo("教学管理");
            assertThat(application.getSystemManaged()).isTrue();
        });
        assertThat(endpointCatalog.endpoints()).extracting(endpoint -> endpoint.definition().endpointId())
                .contains("education.student.enable.enable", "education.student.recycleBin.query",
                        "education.teacher.enable.disable", "education.classroom.sort.sort",
                        "education.classroom.recycleBin.restore", "education.hobby.tree.tree",
                        "education.hobby.tree.sort");
    }

    @Test
    void shouldSupportTreeHobbiesAndResolveStudentMultiSelectTitles() {
        String serial = serial();
        try (TenantContext.Scope ignored = TenantContext.use("campus-hobby")) {
            String sportId = hobbies.insert(new Hobby("sport-" + serial, "运动", TreeAbility.ROOT_ID));
            String basketballId = hobbies.insert(new Hobby("basketball-" + serial, "篮球", sportId));
            String readingId = hobbies.insert(new Hobby("reading-" + serial, "阅读", TreeAbility.ROOT_ID));
            Student student = new Student("S-" + serial, "爱好学生", "五年级");
            student.setHobbyIds(new LinkedHashSet<>(List.of(basketballId, readingId)));
            String studentId = students.insert(student);

            assertThat(hobbies.children(sportId)).extracting(Hobby::getId).containsExactly(basketballId);
            Student selected = students.select(studentId);
            assertThat(selected.getHobbyIds()).containsExactlyInAnyOrder(basketballId, readingId);
            assertThat(selected.getHobbyTitles()).containsExactlyInAnyOrder("篮球", "阅读");

            Hobby basketball = hobbies.select(basketballId);
            basketball.setTitle("篮球校队");
            assertThat(hobbies.update(basketball)).isEqualTo(1);
            assertThat(students.select(studentId).getHobbyTitles())
                    .containsExactlyInAnyOrder("篮球校队", "阅读");
        }
    }

    @Test
    void shouldUsePlatformDictionaryForTeacherTeachingSubject() {
        try (TenantContext.Scope ignored = TenantContext.system("school demo dictionary")) {
            assertThat(optionSources.source(OptionBinding.dictionary("education", "teaching_subject"))
                    .options(OptionQuery.enabledOnly()))
                    .extracting(option -> option.code(), option -> option.title())
                    .containsExactlyInAnyOrder(
                            org.assertj.core.groups.Tuple.tuple("mathematics", "数学"),
                            org.assertj.core.groups.Tuple.tuple("chinese", "语文"),
                            org.assertj.core.groups.Tuple.tuple("english", "英语"));

            String teacherId = teachers.insert(new Teacher("T-" + serial(), "数学老师", "mathematics"));
            Teacher output = recordOutput.record(teachers, teachers.select(teacherId), FieldOutputContext.VIEW);
            assertThat(output.getSubjectCode()).isEqualTo("mathematics");
            assertThat(output.getSubjectCodeTitle()).isEqualTo("数学");

            assertThatThrownBy(() -> teachers.insert(new Teacher("T-" + serial(), "无效学科", "physics")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("invalid option code for field subjectCode");

            String anotherTeacherId = teachers.insert(new Teacher("T-" + serial(), "英语老师", "english"));
            Teacher anotherTeacher = teachers.select(anotherTeacherId);
            String duplicateTeacherNo = teachers.select(teacherId).getTeacherNo();
            anotherTeacher.setTeacherNo(duplicateTeacherNo);
            assertThatThrownBy(() -> teachers.update(anotherTeacher))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("teacherNo already exists");
        }
    }

    @Test
    void shouldKeepStudentTenantScopeAndDemonstrateEnableCacheAndRecycleBin() {
        String serial = serial();
        String campusAStudentId;
        try (TenantContext.Scope ignored = TenantContext.use("campus-a")) {
            campusAStudentId = students.insert(new Student("S-" + serial, "林晓", "一年级"));

            Student firstRead = students.select(campusAStudentId);
            firstRead.setTitle("mutated client copy");
            assertThat(students.select(campusAStudentId).getTitle()).isEqualTo("林晓");

            assertThatThrownBy(() -> students.insert(new Student("S-" + serial, "重复学号", "一年级")))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("studentNo already exists");
            assertThat(students.disable(campusAStudentId)).isEqualTo(1);
            assertThat(students.isEnabled(campusAStudentId)).isFalse();
        }

        try (TenantContext.Scope ignored = TenantContext.use("campus-b")) {
            students.insert(new Student("S-" + serial, "周然", "一年级"));
            assertThat(students.list(Criteria.of())).extracting(Student::getTitle).contains("周然");
        }

        try (TenantContext.Scope ignored = TenantContext.use("campus-a")) {
            assertThat(students.delete(campusAStudentId)).isEqualTo(1);
            assertThat(students.select(campusAStudentId)).isNull();
            assertThat(students.pageRecycleBin(Criteria.of(), PageRequest.of(1, 10)).getRecords())
                    .extracting(Student::getTitle).contains("林晓");
            assertThat(students.restore(campusAStudentId)).isEqualTo(1);
            assertThat(students.select(campusAStudentId).getTenantId()).isEqualTo("campus-a");
        }
    }

    @Test
    void shouldResolveHomeroomTeacherAndPopulateClassMembers() {
        try (TenantContext.Scope ignored = TenantContext.system("school demo aggregate")) {
            String teacherId = teachers.insert(new Teacher("T-" + serial(), "王老师", "mathematics"));
            String studentId = students.insert(new Student("S-" + serial(), "陈同学", "二年级"));
            ClassMember member = new ClassMember(studentId);
            Classroom classroom = new Classroom("G2-" + serial(), "二年级一班", "2026", teacherId);
            classroom.setMembers(List.of(member));

            String classroomId = classrooms.insert(classroom);
            classroom.setMembers(null);
            Classroom selected = classrooms.select(classroomId);

            assertThat(selected.getHomeroomTeacherIdTitle()).isEqualTo("王老师");
            assertThat(selected.getMembers()).singleElement().satisfies(loaded -> {
                assertThat(loaded.getClassroomId()).isEqualTo(classroomId);
                assertThat(loaded.getSortOrder()).isEqualTo(100);
            });
            assertThat(members.select(member.getId()).getStudentIdTitle()).isEqualTo("陈同学");
        }
    }

    @Test
    void shouldReplaceMemberRowsAndCascadeSoftDeleteWhenClassroomIsDeleted() {
        try (TenantContext.Scope ignored = TenantContext.system("school demo aggregate")) {
            String teacherId = teachers.insert(new Teacher("T-" + serial(), "王老师", "mathematics"));
            String firstStudentId = students.insert(new Student("S-" + serial(), "林晓", "三年级"));
            String removedStudentId = students.insert(new Student("S-" + serial(), "周然", "三年级"));
            String replacementStudentId = students.insert(new Student("S-" + serial(), "陈同学", "三年级"));
            ClassMember first = new ClassMember(firstStudentId);
            ClassMember removed = new ClassMember(removedStudentId);
            Classroom classroom = new Classroom("G3-" + serial(), "三年级一班", "2026", teacherId);
            classroom.setMembers(List.of(first, removed));
            String classroomId = classrooms.insert(classroom);

            ClassMember replacement = new ClassMember(replacementStudentId);
            classroom.setMembers(List.of(first, replacement));
            assertThat(classrooms.update(classroom)).isEqualTo(1);
            assertThat(members.select(removed.getId())).isNull();
            assertThat(members.selectIgnoreSoftDelete(removed.getId())).isNotNull();

            assertThat(classrooms.delete(classroomId)).isEqualTo(1);
            assertThat(classrooms.select(classroomId)).isNull();
            assertThat(classrooms.pageRecycleBin(Criteria.of(), PageRequest.of(1, 10)).getRecords())
                    .extracting(Classroom::getId).contains(classroomId);
            assertThat(members.select(first.getId())).isNull();
            assertThat(members.select(replacement.getId())).isNull();
            assertThat(members.selectIgnoreSoftDelete(first.getId())).isNotNull();
            assertThat(members.selectIgnoreSoftDelete(replacement.getId())).isNotNull();
        }
    }

    @Test
    void shouldOrderClassroomsByAcademicYearAndMembersByClassroom() {
        try (TenantContext.Scope ignored = TenantContext.system("school demo sort")) {
            String teacherId = teachers.insert(new Teacher("T-" + serial(), "王老师", "mathematics"));
            String firstStudentId = students.insert(new Student("S-" + serial(), "林晓", "四年级"));
            String secondStudentId = students.insert(new Student("S-" + serial(), "周然", "四年级"));
            Classroom first = new Classroom("G4-" + serial(), "四年级一班", "2026", teacherId);
            first.setMembers(List.of(new ClassMember(firstStudentId), new ClassMember(secondStudentId)));
            String firstId = classrooms.insert(first);
            Classroom second = new Classroom("G4-" + serial(), "四年级二班", "2026", teacherId);
            String secondId = classrooms.insert(second);

            assertThat(first.getSortOrder()).isPositive();
            assertThat(second.getSortOrder()).isGreaterThan(first.getSortOrder());
            assertThat(first.getMembers()).extracting(ClassMember::getSortOrder).containsExactly(100, 200);

            classrooms.moveBefore(secondId, firstId);
            assertThat(classrooms.sortedList(Criteria.of())).extracting(Classroom::getTitle)
                    .containsSubsequence("四年级二班", "四年级一班");
        }
    }

    private String serial() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
