package net.ximatai.muyun.spring.boot.demo.school.classroom;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.ability.reference.ReferenceIntegrity;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;
import net.ximatai.muyun.spring.ability.reference.ReferenceLoad;
import net.ximatai.muyun.spring.ability.reference.ReferenceHop;
import net.ximatai.muyun.spring.ability.child.ChildOf;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetUnavailablePolicy;
import net.ximatai.muyun.spring.ability.SortPartitionBy;
import net.ximatai.muyun.spring.common.model.capability.SortCapable;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.boot.demo.school.student.StudentService;
import net.ximatai.muyun.spring.boot.demo.school.teacher.TeacherService;

/**
 * 班级成员子表：记录学生与班级之间的关系事实。
 * 成员在各自班级内排序，展示 {@code @SortPartitionBy} 的子表作用域声明。
 */
@Getter
@Setter
@Table(name = "education_class_member", comment = "班级成员")
@SortPartitionBy(fields = "classroomId")
public class ClassMember extends StandardEntity implements SortCapable {
    /**
     * 成员归属班级：{@code @ChildOf} 建立与 {@link Classroom#members} 的聚合所有权；
     * 班级被删除时，引用完整性策略级联删除成员事实。
     */
    @ReferenceTo(
            target = ClassroomService.class,
            integrity = @ReferenceIntegrity(onTargetUnavailable = ReferenceTargetUnavailablePolicy.CASCADE_DELETE)
    )
    @ChildOf
    @Column(name = "classroom_id", type = ColumnType.VARCHAR, length = 32, nullable = false)
    private String classroomId;

    /** 学生退出或删除前必须先处理班级成员记录，演示 {@code RESTRICT} 引用完整性。 */
    @ReferenceTo(target = StudentService.class,
            integrity = @ReferenceIntegrity(onTargetUnavailable = ReferenceTargetUnavailablePolicy.RESTRICT))
    @Column(name = "student_id", type = ColumnType.VARCHAR, length = 32, nullable = false)
    private String studentId;

    /** 从学生引用直接读取标题，作为单跳 {@code @ReferenceLoad} 的最小示例。 */
    @ReferenceLoad(source = "studentId", field = "title")
    private transient String studentIdTitle;

    /**
     * 班主任可跨班复用同一位学生助理：成员从班级取班主任，再从教师取学生助理，
     * 最终投影助理姓名，演示类型化多 hop {@code @ReferenceLoad}。
     */
    @ReferenceLoad(source = "classroomId", hops = {
            @ReferenceHop(target = TeacherService.class, via = "homeroomTeacherId"),
            @ReferenceHop(target = StudentService.class, via = "studentAssistantId")
    })
    private transient String homeroomTeacherAssistantTitle;

    @Column(name = "sort_order", type = ColumnType.INT, comment = "Sort order")
    private Integer sortOrder;
}
