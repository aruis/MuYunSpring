package net.ximatai.muyun.spring.boot.demo.school.teacher;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.annotation.TrueOrFalse;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.option.OptionField;
import net.ximatai.muyun.spring.common.option.OptionSourceType;
import net.ximatai.muyun.spring.common.model.standard.StandardTitledEntity;
import net.ximatai.muyun.spring.common.model.constraint.TenantUniqueConstraint;

/** 教师主数据；班级主表通过班主任引用关联教师，教学学科则保存平台字典项 code。 */
@Getter
@Setter
@Table(name = "education_teacher", comment = "教师")
@TenantUniqueConstraint(fields = "teacherNo", message = "teacherNo already exists in the current tenant")
public class Teacher extends StandardTitledEntity implements EnabledCapable {
    @Column(name = "teacher_no", type = ColumnType.VARCHAR, length = 32, nullable = false)
    private String teacherNo;

    @OptionField(type = OptionSourceType.DICTIONARY, source = "education.teaching_subject")
    @Column(name = "subject_code", type = ColumnType.VARCHAR, length = 64, nullable = false)
    private String subjectCode;

    private transient String subjectCodeTitle;

    @Column(name = "enabled", type = ColumnType.BOOLEAN, nullable = false,
            defaultVal = @net.ximatai.muyun.database.core.annotation.Default(bool = TrueOrFalse.TRUE))
    private Boolean enabled = Boolean.TRUE;

    public Teacher() {
    }

    public Teacher(String teacherNo, String title, String subjectCode) {
        this.teacherNo = teacherNo;
        setTitle(title);
        this.subjectCode = subjectCode;
    }
}
