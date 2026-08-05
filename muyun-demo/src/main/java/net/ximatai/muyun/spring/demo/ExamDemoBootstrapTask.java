package net.ximatai.muyun.spring.demo;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.demo.school.student.Student;
import net.ximatai.muyun.spring.demo.school.student.StudentService;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.metadata.Metadata;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.MetadataService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import net.ximatai.muyun.spring.platform.metadata.RelationRole;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import net.ximatai.muyun.spring.platform.runtime.PlatformBootstrapTask;
import net.ximatai.muyun.spring.platform.runtime.PlatformDynamicRuntimeRefreshService;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Creates a small dynamic aggregate beside the static education student master data. */
public class ExamDemoBootstrapTask implements PlatformBootstrapTask {
    public static final String MODULE_ALIAS = "education.exam";
    private static final String EXAM_METADATA_ALIAS = "exam";
    private static final String PARTICIPANT_METADATA_ALIAS = "exam_participant";
    private static final String PARTICIPANT_RELATION_ALIAS = "participants";
    private static final PageRequest ONE = new PageRequest(0, 1);

    private final PlatformModuleService moduleService;
    private final MetadataService metadataService;
    private final MetadataFieldService fieldService;
    private final ModuleMetadataRelationService relationService;
    private final DynamicRecordService recordService;
    private final StudentService studentService;
    private final PlatformDynamicRuntimeRefreshService runtimeRefreshService;
    private final TransactionTemplate transactionTemplate;

    public ExamDemoBootstrapTask(PlatformModuleService moduleService,
                                 MetadataService metadataService,
                                 MetadataFieldService fieldService,
                                 ModuleMetadataRelationService relationService,
                                 DynamicRecordService recordService,
                                 StudentService studentService,
                                 PlatformDynamicRuntimeRefreshService runtimeRefreshService,
                                 TransactionTemplate transactionTemplate) {
        this.moduleService = moduleService;
        this.metadataService = metadataService;
        this.fieldService = fieldService;
        this.relationService = relationService;
        this.recordService = recordService;
        this.studentService = studentService;
        this.runtimeRefreshService = runtimeRefreshService;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public String name() {
        return "demo-exam-metadata";
    }

    @Override
    public int order() {
        return 210;
    }

    @Override
    public void run() {
        try (CurrentUserContext.Scope ignored = CurrentUserContext.use(
                CurrentUser.systemUser("demo-exam-bootstrap", "Exam Demo Bootstrap"))) {
            try (TenantContext.Scope ignoredTenant = TenantContext.system("configure exam demo metadata")) {
                // 元数据、字段和关系在同一事务内提交，schema ensure 在事务提交后按完整实体一次建表，
                // 避免逐条保存时向已存在表追加 NOT NULL 列被严格迁移拒绝。
                transactionTemplate.executeWithoutResult(status -> configureMetadata());
                // 元数据事务提交后刷新动态运行态：编译模块定义、建表并注册到内存注册表，
                // 否则后续创建记录时注册表仍不认识该模块；重启场景同样需要该刷新。
                runtimeRefreshService.refresh(MODULE_ALIAS);
            }
            try (TenantContext.Scope ignoredTenant = TenantContext.use(DemoBootstrapTask.TENANT_ALIAS)) {
                Student firstStudent = ensureStudent("demo_student_1001", "S2026001", "陈晨", "高一");
                Student secondStudent = ensureStudent("demo_student_1002", "S2026002", "林晓", "高一");
                ensureExamRecords(firstStudent, secondStudent);
            }
        }
    }

    private void configureMetadata() {
        ensureModule();
        Metadata exam = ensureMetadata(EXAM_METADATA_ALIAS, "考试", "education_exam");
        ensureField(exam.getId(), "title", "title", "string", "考试名称", true, true);
        ensureField(exam.getId(), "subject", "subject", "string", "科目", true, false);
        ensureField(exam.getId(), "examDate", "exam_date", "date", "考试日期", true, false);
        ModuleMetadataRelation main = ensureMainRelation(exam.getId());

        Metadata participant = ensureMetadata(PARTICIPANT_METADATA_ALIAS, "参考学生", "education_exam_participant");
        ensureField(participant.getId(), "examId", "exam_id", "string", "考试", true, false);
        ensureField(participant.getId(), "studentId", "student_id", "string", "学生 ID", true, false);
        ensureField(participant.getId(), "studentNo", "student_no", "string", "学号", true, false);
        ensureField(participant.getId(), "studentName", "student_name", "string", "学生姓名", true, false);
        ensureField(participant.getId(), "score", "score", "decimal", "成绩", false, false);
        ensureField(participant.getId(), "attendanceStatus", "attendance_status", "string", "参考状态", true, false);
        ensureChildRelation(participant.getId(), main.getMetadataId());
    }

    private void ensureModule() {
        if (moduleService.select(MODULE_ALIAS) != null) {
            return;
        }
        PlatformModule module = new PlatformModule();
        module.setAlias(MODULE_ALIAS);
        module.setApplicationAlias("education");
        module.setModuleKind(ModuleKind.DYNAMIC);
        module.setTitle("考试管理");
        moduleService.insert(module);
    }

    private Metadata ensureMetadata(String alias, String title, String tableName) {
        Metadata existing = metadataService.list(Criteria.of()
                .eq("applicationAlias", "education")
                .eq("alias", alias), ONE).stream().findFirst().orElse(null);
        if (existing != null) {
            return existing;
        }
        Metadata metadata = new Metadata();
        metadata.setApplicationAlias("education");
        metadata.setAlias(alias);
        metadata.setTitle(title);
        metadata.setTableName(tableName);
        String id = metadataService.insert(metadata);
        return metadataService.select(id);
    }

    private void ensureField(String metadataId, String fieldName, String columnName, String fieldSpecAlias,
                             String title, boolean required, boolean titleField) {
        if (!fieldService.list(Criteria.of().eq("metadataId", metadataId).eq("fieldName", fieldName), ONE)
                .isEmpty()) {
            return;
        }
        MetadataField field = new MetadataField();
        field.setMetadataId(metadataId);
        field.setFieldName(fieldName);
        field.setColumnName(columnName);
        field.setFieldSpecAlias(fieldSpecAlias);
        field.setTitle(title);
        field.setRequired(required);
        field.setTitleField(titleField);
        fieldService.insert(field);
    }

    private ModuleMetadataRelation ensureMainRelation(String metadataId) {
        ModuleMetadataRelation existing = relationService.list(Criteria.of()
                .eq("moduleAlias", MODULE_ALIAS)
                .eq("relationRole", RelationRole.MAIN), ONE).stream().findFirst().orElse(null);
        if (existing != null) {
            return existing;
        }
        ModuleMetadataRelation relation = new ModuleMetadataRelation();
        relation.setModuleAlias(MODULE_ALIAS);
        relation.setMetadataId(metadataId);
        relation.setRelationRole(RelationRole.MAIN);
        relation.setRelationAlias(EXAM_METADATA_ALIAS);
        relation.setTitle("考试");
        String id = relationService.insert(relation);
        return relationService.select(id);
    }

    private void ensureChildRelation(String metadataId, String parentMetadataId) {
        if (!relationService.list(Criteria.of().eq("moduleAlias", MODULE_ALIAS)
                .eq("metadataId", metadataId).eq("relationRole", RelationRole.CHILD), ONE).isEmpty()) {
            return;
        }
        ModuleMetadataRelation relation = new ModuleMetadataRelation();
        relation.setModuleAlias(MODULE_ALIAS);
        relation.setMetadataId(metadataId);
        relation.setRelationRole(RelationRole.CHILD);
        relation.setRelationAlias(PARTICIPANT_RELATION_ALIAS);
        relation.setTitle("参考学生");
        relation.setParentMetadataId(parentMetadataId);
        relation.setForeignKey("examId");
        relation.setAutoPopulate(Boolean.TRUE);
        relationService.insert(relation);
    }

    private Student ensureStudent(String id, String studentNo, String title, String grade) {
        Student existing = studentService.selectIgnoreSoftDelete(id);
        if (existing != null) {
            return existing;
        }
        Student student = new Student();
        student.setId(id);
        student.setStudentNo(studentNo);
        student.setTitle(title);
        student.setGrade(grade);
        student.setEnabled(Boolean.TRUE);
        studentService.insert(student);
        return student;
    }

    private void ensureExamRecords(Student firstStudent, Student secondStudent) {
        if (recordService.mainEntity(MODULE_ALIAS).count(Criteria.of().eq("title", "2026 春季期中考试")) > 0) {
            return;
        }
        DynamicRecord firstExam = recordService.newRecord(MODULE_ALIAS, EXAM_METADATA_ALIAS)
                .setValue("title", "2026 春季期中考试")
                .setValue("subject", "数学")
                .setValue("examDate", LocalDate.of(2026, 4, 18));
        firstExam.setChildren(PARTICIPANT_RELATION_ALIAS, List.of(
                participant(firstStudent, new BigDecimal("92.5"), "ATTENDED"),
                participant(secondStudent, new BigDecimal("86"), "ATTENDED")));
        recordService.mainEntity(MODULE_ALIAS).create(firstExam);

        DynamicRecord secondExam = recordService.newRecord(MODULE_ALIAS, EXAM_METADATA_ALIAS)
                .setValue("title", "2026 春季英语听力测试")
                .setValue("subject", "英语")
                .setValue("examDate", LocalDate.of(2026, 5, 8));
        secondExam.setChildren(PARTICIPANT_RELATION_ALIAS, List.of(
                participant(firstStudent, new BigDecimal("95"), "ATTENDED"),
                participant(secondStudent, null, "ABSENT")));
        recordService.mainEntity(MODULE_ALIAS).create(secondExam);
    }

    private DynamicRecord participant(Student student, BigDecimal score, String attendanceStatus) {
        DynamicRecord participant = recordService.newRecord(MODULE_ALIAS, PARTICIPANT_METADATA_ALIAS)
                .setValue("studentId", student.getId())
                .setValue("studentNo", student.getStudentNo())
                .setValue("studentName", student.getTitle())
                .setValue("attendanceStatus", attendanceStatus);
        if (score != null) {
            participant.setValue("score", score);
        }
        return participant;
    }
}
