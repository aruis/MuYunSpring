package net.ximatai.muyun.spring.platform.workflow;

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
@Table(name = "platform_workflow_definition", comment = "Workflow definition")
@CompositeIndex(columns = {"tenant_id", "module_alias", "alias"}, unique = true)
public class WorkflowDefinition extends StandardEnabledSortableEntity {
    @Column(name = "application_alias", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "Application alias")
    private String applicationAlias;

    @Column(name = "module_alias", type = ColumnType.VARCHAR, length = 128, nullable = false, comment = "Module alias")
    private String moduleAlias;

    @Column(name = "entity_alias", type = ColumnType.VARCHAR, length = 64, comment = "Entity alias")
    private String entityAlias;

    @Column(name = "alias", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Workflow alias")
    private String alias;

    @Column(name = "approval_enabled", type = ColumnType.BOOLEAN, nullable = false,
            comment = "Whether workflow governs approval status", defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean approvalEnabled = Boolean.FALSE;

    @Column(name = "definition_status", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Definition status", defaultVal = @Default(varchar = "draft"))
    private WorkflowDefinitionStatus definitionStatus = WorkflowDefinitionStatus.DRAFT;

    @Column(name = "current_version_no", type = ColumnType.INT, comment = "Current published version number")
    private Integer currentVersionNo;
}
