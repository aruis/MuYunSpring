package net.ximatai.muyun.spring.iam.role;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.annotation.TrueOrFalse;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;

@Getter
@Setter
@Table(name = "iam_role", comment = "Role")
@CompositeIndex(columns = {"tenant_id", "role_kind", "title"}, unique = true)
public class Role extends StandardEnabledSortableEntity {
    @Column(name = "role_kind", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Role kind",
            defaultVal = @Default(varchar = "standard"))
    private RoleKind roleKind = RoleKind.STANDARD;

    @Column(name = "member_role_ids", type = ColumnType.TEXT, comment = "Member role ids for role group")
    private String memberRoleIds;

    @Column(name = "grant_subject_types", type = ColumnType.VARCHAR, length = 128, nullable = false,
            comment = "Allowed grant subject types", defaultVal = @Default(varchar = "userAccount"))
    private String grantSubjectTypes = RoleGrantSubjectType.USER_ACCOUNT.getCode();

    @Column(name = "public_role", type = ColumnType.BOOLEAN, comment = "Visible to child management scopes",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean publicRole = Boolean.FALSE;

    @Column(name = "built_in", type = ColumnType.BOOLEAN, comment = "Built-in role flag",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean builtIn = Boolean.FALSE;

    @Column(name = "system_managed", type = ColumnType.BOOLEAN, comment = "System managed role flag",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean systemManaged = Boolean.FALSE;

    @Column(name = "description", type = ColumnType.TEXT, comment = "Role description")
    private String description;
}
