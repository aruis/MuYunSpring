package net.ximatai.muyun.spring.common.model.standard;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.capability.DataScopeCapable;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;

@Getter
@Setter
public abstract class StandardDataScopedEnabledSortableEntity extends StandardEnabledSortableEntity implements DataScopeCapable {
    @Column(name = PlatformAbilityFields.AUTH_USER_COLUMN,
            type = ColumnType.VARCHAR,
            length = PlatformAbilityFields.AUTH_USER_LENGTH,
            comment = "Data scope owner user id")
    private String authUserId;

    @Column(name = PlatformAbilityFields.AUTH_ASSIGNEE_COLUMN,
            type = ColumnType.TEXT,
            comment = "Data scope assignee ids")
    private String authAssigneeIds;

    @Column(name = PlatformAbilityFields.AUTH_MEMBER_COLUMN,
            type = ColumnType.TEXT,
            comment = "Data scope member ids")
    private String authMemberIds;

    @Column(name = PlatformAbilityFields.AUTH_ORGANIZATION_COLUMN,
            type = ColumnType.VARCHAR,
            length = PlatformAbilityFields.AUTH_ORGANIZATION_LENGTH,
            comment = "Data scope organization id")
    private String authOrganizationId;

    @Column(name = PlatformAbilityFields.AUTH_MODULE_COLUMN,
            type = ColumnType.VARCHAR,
            length = PlatformAbilityFields.AUTH_MODULE_LENGTH,
            comment = "Data scope module alias")
    private String authModuleAlias;
}
