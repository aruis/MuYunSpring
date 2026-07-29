package net.ximatai.muyun.spring.iam.department;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.initialdata.InitialDataFields;
import net.ximatai.muyun.spring.common.model.constraint.TenantUniqueConstraint;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledTreeEntity;

@Getter
@Setter
@Table(name = "iam_department", comment = "Department")
@TenantUniqueConstraint(fields = {"organizationId", "code"})
@InitialDataFields(managed = {"organizationId", "code"}, operator = {"title", "enabled", "sortOrder", "parentId"})
public class Department extends StandardEnabledTreeEntity {
    @Column(name = "organization_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Organization id")
    private String organizationId;

    @Column(name = "code", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Department code")
    private String code;
}
