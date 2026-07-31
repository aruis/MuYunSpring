package net.ximatai.muyun.spring.boot.demo.school.classroom;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.ability.child.Children;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;
import net.ximatai.muyun.spring.ability.reference.ReferenceLoad;
import net.ximatai.muyun.spring.ability.reference.ReferenceHop;
import net.ximatai.muyun.spring.ability.SortPartitionBy;
import net.ximatai.muyun.spring.common.model.standard.StandardSortableEntity;
import net.ximatai.muyun.spring.boot.demo.school.teacher.TeacherService;

import java.util.List;

/**
 * 班级聚合：主表保存班主任，成员信息通过自动维护的子表保存。
 * 班级仅在同一学年内排序，避免跨学年互相影响。
 */
@Getter
@Setter
@Table(name = "education_classroom", comment = "班级")
@SortPartitionBy(fields = "academicYear")
public class Classroom extends StandardSortableEntity {
    @Column(name = "class_code", type = ColumnType.VARCHAR, length = 32, nullable = false)
    private String classCode;

    @Column(name = "academic_year", type = ColumnType.VARCHAR, length = 16, nullable = false)
    private String academicYear;

    /** 班级引用班主任，供班主任选择与后续多跳读取使用。 */
    @ReferenceTo(target = TeacherService.class)
    @Column(name = "homeroom_teacher_id", type = ColumnType.VARCHAR, length = 32, nullable = false)
    private String homeroomTeacherId;

    /** 直接投影班主任姓名，避免业务代码手工填充展示字段。 */
    @ReferenceLoad(source = "homeroomTeacherId", field = "title")
    private transient String homeroomTeacherIdTitle;

    /** 班级是成员聚合根；保存班级时平台自动维护成员的 {@code classroomId}。 */
    @Children
    private List<ClassMember> members;

}
