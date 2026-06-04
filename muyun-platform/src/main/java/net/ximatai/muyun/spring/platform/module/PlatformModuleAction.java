package net.ximatai.muyun.spring.platform.module;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.annotation.TrueOrFalse;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;

@Getter
@Setter
@Table(name = "platform_module_action", comment = "Platform module action")
@CompositeIndex(columns = {"module_alias", "action_code"}, unique = true)
public class PlatformModuleAction extends StandardEnabledSortableEntity {
    @Column(name = "module_alias", type = ColumnType.VARCHAR, length = 128, nullable = false, comment = "Module alias")
    private String moduleAlias;

    @Column(name = "action_code", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Action code")
    private String actionCode;

    @Column(name = "permission_action_code", type = ColumnType.VARCHAR, length = 64,
            comment = "Permission action code")
    private String permissionActionCode;

    @Column(name = "title", type = ColumnType.VARCHAR, length = 128, nullable = false, comment = "Action title")
    private String title;

    @Column(name = "action_level", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Action execution level")
    private EntityActionLevel actionLevel;

    @Column(name = "access_mode", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Action access mode", defaultVal = @Default(varchar = "AUTH_REQUIRED"))
    private EntityActionAccessMode accessMode = EntityActionAccessMode.AUTH_REQUIRED;

    @Column(name = "action_auth", comment = "Whether action permission applies",
            defaultVal = @Default(bool = TrueOrFalse.TRUE))
    private Boolean actionAuth = Boolean.TRUE;

    @Column(name = "data_auth", comment = "Whether data permission applies",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean dataAuth = Boolean.FALSE;

    @Column(name = "default_grant_policy", type = ColumnType.VARCHAR, length = 32,
            comment = "Default action grant policy")
    private ActionDefaultGrantPolicy defaultGrantPolicy;

    @Column(name = "system_managed", comment = "Whether action is managed by platform",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean systemManaged = Boolean.FALSE;
}
