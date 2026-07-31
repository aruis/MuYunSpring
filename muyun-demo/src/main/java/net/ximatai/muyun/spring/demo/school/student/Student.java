package net.ximatai.muyun.spring.demo.school.student;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.annotation.TrueOrFalse;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;
import net.ximatai.muyun.spring.ability.reference.ReferenceLoad;
import net.ximatai.muyun.spring.ability.reference.ReferencedBy;
import net.ximatai.muyun.spring.demo.school.classroom.ClassMember;
import net.ximatai.muyun.spring.demo.school.hobby.HobbyService;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.model.standard.StandardTitledEntity;
import net.ximatai.muyun.spring.common.model.constraint.TenantUniqueConstraint;

import java.util.List;
import java.util.Set;

/** 学生主数据：学号、显示名称、年级、启停状态和可多选的爱好引用。 */
@Getter
@Setter
@Table(name = "education_student", comment = "学生")
@TenantUniqueConstraint(fields = "studentNo", message = "studentNo already exists in the current tenant")
public class Student extends StandardTitledEntity implements EnabledCapable {
    /**
     * 只读查看学生所在班级的成员事实；班级仍是聚合根，学生不反向拥有或维护成员集合。
     * 演示 {@code @ReferencedBy} 从 {@link ClassMember#studentId} 自动装配反向记录。
     */
    @ReferencedBy
    private transient List<ClassMember> classMemberships;

    @Column(name = "student_no", type = ColumnType.VARCHAR, length = 32, nullable = false)
    private String studentNo;

    @Column(name = "grade", type = ColumnType.VARCHAR, length = 32, nullable = false)
    private String grade;

    /** 学生可跨爱好树分支多选，演示 {@code MANY} 引用值使用 JSON 集合保存。 */
    @ReferenceTo(target = HobbyService.class, cardinality = ReferenceCardinality.MANY)
    @Column(name = "hobby_ids", type = ColumnType.JSON_SET)
    private Set<String> hobbyIds;

    /** 为当前多选值加载对应爱好标题，作为 {@code MANY} 引用的只读展示投影。 */
    @ReferenceLoad(source = "hobbyIds", field = "title")
    private transient List<String> hobbyTitles;

    @Column(name = "enabled", type = ColumnType.BOOLEAN, nullable = false,
            defaultVal = @net.ximatai.muyun.database.core.annotation.Default(bool = TrueOrFalse.TRUE))
    private Boolean enabled = Boolean.TRUE;

}
