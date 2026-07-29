package net.ximatai.muyun.spring.boot.demo.school.student;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.annotation.TrueOrFalse;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;
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
    @Column(name = "student_no", type = ColumnType.VARCHAR, length = 32, nullable = false)
    private String studentNo;

    @Column(name = "grade", type = ColumnType.VARCHAR, length = 32, nullable = false)
    private String grade;

    @ReferenceTo(moduleAlias = "education", entityAlias = "hobby", cardinality = ReferenceCardinality.MANY,
            autoTitle = true, titleOutputField = "hobbyTitles")
    @Column(name = "hobby_ids", type = ColumnType.JSON_SET)
    private Set<String> hobbyIds;

    private transient List<String> hobbyTitles;

    @Column(name = "enabled", type = ColumnType.BOOLEAN, nullable = false,
            defaultVal = @net.ximatai.muyun.database.core.annotation.Default(bool = TrueOrFalse.TRUE))
    private Boolean enabled = Boolean.TRUE;

}
