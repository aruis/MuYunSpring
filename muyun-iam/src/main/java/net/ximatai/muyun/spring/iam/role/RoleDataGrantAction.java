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

/**
 * Data-scope template keyed by a canonical platform permission action.
 *
 * <p>Unlike {@link RoleAction}, this fact never grants an action. It is consulted only when a
 * standard employment role explicitly inherits data authorization from a data-grant role.</p>
 */
@Getter
@Setter
@Table(name = "iam_role_data_grant_action", comment = "Role data grant action template")
@CompositeIndex(columns = {"tenant_id", "role_id", "action_code"}, unique = true)
public class RoleDataGrantAction extends StandardEntity {
    @Column(name = "role_id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Role id")
    private String roleId;

    @Column(name = "action_code", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "Canonical permission action code")
    private String actionCode;

    @Column(name = "data_scope_policy", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Concrete data scope policy")
    private DataScopePolicy dataScopePolicy;

    @Column(name = "enabled", type = ColumnType.BOOLEAN, nullable = false, comment = "Enabled flag",
            defaultVal = @Default(bool = TrueOrFalse.TRUE))
    private Boolean enabled = Boolean.TRUE;
}
