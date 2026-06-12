package net.ximatai.muyun.spring.iam.department;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledTreeEntity;

@Getter
@Setter
@Table(name = "iam_department", comment = "Department")
@CompositeIndex(columns = {"tenant_id", "organization_id", "code"}, unique = true)
public class Department extends StandardEnabledTreeEntity {
    @Column(name = "organization_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Organization id")
    private String organizationId;

    @Column(name = "code", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Department code")
    private String code;
}
