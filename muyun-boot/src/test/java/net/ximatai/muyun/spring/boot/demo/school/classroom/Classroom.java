package net.ximatai.muyun.spring.boot.demo.school.classroom;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.ability.child.ChildRef;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;
import net.ximatai.muyun.spring.common.model.standard.StandardSortableEntity;

import java.util.List;

/** 班级聚合：主表保存班主任，成员信息通过自动维护的子表保存。 */
@Getter
@Setter
@Table(name = "education_classroom", comment = "班级")
public class Classroom extends StandardSortableEntity {
    @Column(name = "class_code", type = ColumnType.VARCHAR, length = 32, nullable = false)
    private String classCode;

    @Column(name = "academic_year", type = ColumnType.VARCHAR, length = 16, nullable = false)
    private String academicYear;

    @ReferenceTo(moduleAlias = "education", entityAlias = "teacher", autoTitle = true)
    @Column(name = "homeroom_teacher_id", type = ColumnType.VARCHAR, length = 32, nullable = false)
    private String homeroomTeacherId;

    private transient String homeroomTeacherIdTitle;

    @ChildRef(
            parentEntityAlias = "classroom",
            childModel = ClassMember.class,
            childEntityAlias = "classMember",
            childForeignKeyField = "classroomId",
            autoPopulate = true,
            autoDeleteWithParent = true
    )
    private List<ClassMember> members;

    public Classroom() {
    }

    public Classroom(String classCode, String title, String academicYear, String homeroomTeacherId) {
        this.classCode = classCode;
        setTitle(title);
        this.academicYear = academicYear;
        this.homeroomTeacherId = homeroomTeacherId;
    }
}
