package net.ximatai.muyun.spring.platform.metadata;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.annotation.TrueOrFalse;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionKind;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionStyle;

@Getter
@Setter
@Table(name = "platform_module_metadata_action", comment = "Module metadata action")
@CompositeIndex(columns = {"relation_id", "action_code"}, unique = true)
public class ModuleMetadataAction extends StandardEnabledSortableEntity {
    @Column(name = "relation_id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Module metadata relation id")
    private String relationId;

    @Column(name = "action_code", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Action code")
    private String actionCode;

    @Column(name = "category", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Action category")
    private EntityActionCategory category;

    @Column(name = "action_kind", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Action kind")
    private EntityActionKind actionKind;

    @Column(name = "action_level", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Action execution level")
    private EntityActionLevel actionLevel;

    @Column(name = "action_style", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Action display style", defaultVal = @Default(varchar = "NORMAL"))
    private EntityActionStyle actionStyle = EntityActionStyle.NORMAL;

    @Column(name = "access_mode", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Action access mode", defaultVal = @Default(varchar = "AUTH_REQUIRED"))
    private EntityActionAccessMode accessMode = EntityActionAccessMode.AUTH_REQUIRED;

    @Column(name = "action_auth", comment = "Whether action permission applies")
    private Boolean actionAuth;

    @Column(name = "data_auth", comment = "Whether data permission applies")
    private Boolean dataAuth;

    @Column(name = "auth_inherit_action_code", type = ColumnType.VARCHAR, length = 64, comment = "Inherited action code")
    private String authInheritActionCode;

    @Column(name = "available_expression", type = ColumnType.TEXT, comment = "Availability expression")
    private String availableExpression;

    @Column(name = "unavailable_message", type = ColumnType.VARCHAR, length = 256, comment = "Unavailable message")
    private String unavailableMessage;

    @Column(name = "executor_type", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Action executor type")
    private EntityActionExecutorType executorType;

    @Column(name = "executor_key", type = ColumnType.VARCHAR, length = 128, comment = "Action executor key")
    private String executorKey;

    @Column(name = "target_metadata_id", type = ColumnType.VARCHAR, length = 32, comment = "Target metadata id")
    private String targetMetadataId;

    @Column(name = "config_id", type = ColumnType.VARCHAR, length = 32, comment = "Managed config id")
    private String configId;

    @Column(name = "system_managed", comment = "Whether action is managed by platform",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean systemManaged = Boolean.FALSE;
}
