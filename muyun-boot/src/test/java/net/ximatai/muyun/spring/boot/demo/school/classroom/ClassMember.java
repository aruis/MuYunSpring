package net.ximatai.muyun.spring.boot.demo.school.classroom;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;
import net.ximatai.muyun.spring.common.model.standard.StandardSortableEntity;

/** 班级成员子表：记录一名学生在班级中的成员关系。 */
@Getter
@Setter
@Table(name = "education_class_member", comment = "班级成员")
public class ClassMember extends StandardSortableEntity {
    @Column(name = "classroom_id", type = ColumnType.VARCHAR, length = 32, nullable = false)
    private String classroomId;

    @ReferenceTo(moduleAlias = "education", entityAlias = "student", autoTitle = true)
    @Column(name = "student_id", type = ColumnType.VARCHAR, length = 32, nullable = false)
    private String studentId;

    private transient String studentIdTitle;

    public ClassMember() {
    }

    public ClassMember(String studentId) {
        this.studentId = studentId;
    }
}
