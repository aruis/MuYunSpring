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
@Table(name = "iam_role_action", comment = "Role action permission")
@CompositeIndex(columns = {"tenant_id", "role_id", "module_alias", "action_code"}, unique = true)
public class RoleAction extends StandardEntity {
    @Column(name = "role_id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Role id")
    private String roleId;

    @Column(name = "module_alias", type = ColumnType.VARCHAR, length = 128, nullable = false, comment = "Module alias")
    private String moduleAlias;

    @Column(name = "action_code", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Action code")
    private String actionCode;

    @Column(name = "data_scope_policy", type = ColumnType.VARCHAR, length = 32, comment = "Data scope policy")
    private DataScopePolicy dataScopePolicy;

    @Column(name = "tenant_scope_policy", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Tenant scope policy", defaultVal = @Default(varchar = "currentTenant"))
    private TenantScopePolicy tenantScopePolicy = TenantScopePolicy.CURRENT_TENANT;

    @Column(name = "scope_condition", type = ColumnType.TEXT, comment = "Custom data scope condition")
    private String scopeCondition;

    @Column(name = "reference_field_id", type = ColumnType.VARCHAR, length = 32, comment = "Reference field id")
    private String referenceFieldId;

    @Column(name = "reference_action_code", type = ColumnType.VARCHAR, length = 64, comment = "Reference action code")
    private String referenceActionCode;

    @Column(name = "enabled", type = ColumnType.BOOLEAN, nullable = false, comment = "Enabled flag",
            defaultVal = @Default(bool = TrueOrFalse.TRUE))
    private Boolean enabled = Boolean.TRUE;
}
