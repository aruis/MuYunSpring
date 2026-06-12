package net.ximatai.muyun.spring.iam.role;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.annotation.TrueOrFalse;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;

@Getter
@Setter
@Table(name = "iam_role_grant", comment = "Role grant")
@CompositeIndex(columns = {"tenant_id", "role_id", "subject_type", "subject_id"}, unique = true)
public class RoleGrant extends StandardEntity {
    @Column(name = "role_id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Role id")
    private String roleId;

    @Column(name = "subject_type", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Grant subject type")
    private RoleGrantSubjectType subjectType;

    @Column(name = "subject_id", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Grant subject id")
    private String subjectId;

    @Column(name = "enabled", type = ColumnType.BOOLEAN, nullable = false, comment = "Enabled flag",
            defaultVal = @Default(bool = TrueOrFalse.TRUE))
    private Boolean enabled = Boolean.TRUE;
}
