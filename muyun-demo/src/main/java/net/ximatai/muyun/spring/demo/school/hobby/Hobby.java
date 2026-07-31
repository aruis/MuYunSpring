package net.ximatai.muyun.spring.demo.school.hobby;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledTreeEntity;
import net.ximatai.muyun.spring.common.model.constraint.TenantUniqueConstraint;

/** 租户内维护的爱好分类树；学生可跨分支多选其节点。 */
@Getter
@Setter
@Table(name = "education_hobby", comment = "爱好分类")
@TenantUniqueConstraint(fields = "code", message = "hobby code already exists in the current tenant")
public class Hobby extends StandardEnabledTreeEntity {
    @Column(name = "code", type = ColumnType.VARCHAR, length = 32, nullable = false)
    private String code;

}
