package net.ximatai.muyun.spring.iam.role;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.annotation.TrueOrFalse;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.initialdata.InitialDataFields;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;

@Getter
@Setter
@Table(name = "iam_account_role_grant", comment = "Account role grant")
@CompositeIndex(columns = {"tenant_id", "role_id", "user_id", "management_scope_type", "management_scope_id"},
        unique = true)
@InitialDataFields(
        includeId = false,
        identity = {"roleId", "userId", "managementScopeType", "managementScopeId"},
        managed = {"enabled"}
)
public class AccountRoleGrant extends StandardEntity {
    @Column(name = "role_id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Role id")
    private String roleId;

    @Column(name = "user_id", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "User account id (iam_user.id)")
    private String userId;

    @Column(name = "management_scope_type", type = ColumnType.VARCHAR, length = 32,
            comment = "Management scope type")
    private ManagementScopeType managementScopeType;

    @Column(name = "management_scope_id", type = ColumnType.VARCHAR, length = 64,
            comment = "Management scope id")
    private String managementScopeId;

    @Column(name = "enabled", type = ColumnType.BOOLEAN, nullable = false, comment = "Enabled flag",
            defaultVal = @Default(bool = TrueOrFalse.TRUE))
    private Boolean enabled = Boolean.TRUE;
}
