package net.ximatai.muyun.spring.boot.demo.school.classroom;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.ability.reference.ReferenceIntegrity;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetUnavailablePolicy;
import net.ximatai.muyun.spring.common.model.capability.SortCapable;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;

/** 班级成员子表：记录学生与班级之间的关系事实。 */
@Getter
@Setter
@Table(name = "education_class_member", comment = "班级成员")
public class ClassMember extends StandardEntity implements SortCapable {
    @ReferenceTo(
            moduleAlias = "education",
            entityAlias = "classroom",
            integrity = @ReferenceIntegrity(onTargetUnavailable = ReferenceTargetUnavailablePolicy.CASCADE_DELETE)
    )
    @Column(name = "classroom_id", type = ColumnType.VARCHAR, length = 32, nullable = false)
    private String classroomId;

    @ReferenceTo(
            moduleAlias = "education",
            entityAlias = "student",
            autoTitle = true,
            integrity = @ReferenceIntegrity(onTargetUnavailable = ReferenceTargetUnavailablePolicy.RESTRICT)
    )
    @Column(name = "student_id", type = ColumnType.VARCHAR, length = 32, nullable = false)
    private String studentId;

    private transient String studentIdTitle;

    @Column(name = "sort_order", type = ColumnType.INT, comment = "Sort order")
    private Integer sortOrder;
}
