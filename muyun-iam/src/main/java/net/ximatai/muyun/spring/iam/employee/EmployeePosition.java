package net.ximatai.muyun.spring.iam.employee;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.annotation.TrueOrFalse;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.model.capability.SortCapable;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;

@Getter
@Setter
@Table(name = "iam_employee_position", comment = "Employee position")
@CompositeIndex(columns = {"tenant_id", "employee_id", "organization_id", "department_id", "position_id"},
        unique = true)
public class EmployeePosition extends StandardEntity implements EnabledCapable, SortCapable {
    @Column(name = "employee_id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Employee id")
    private String employeeId;

    @Column(name = "organization_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Organization id")
    private String organizationId;

    @Column(name = "department_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Department id")
    private String departmentId;

    @Column(name = "position_id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Position id")
    private String positionId;

    @Column(name = "primary_position", type = ColumnType.BOOLEAN, comment = "Primary position",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean primaryPosition = Boolean.FALSE;

    @Column(name = PlatformAbilityFields.ENABLED_COLUMN, type = ColumnType.BOOLEAN, comment = "Enabled flag",
            defaultVal = @Default(bool = TrueOrFalse.TRUE))
    private Boolean enabled = Boolean.TRUE;

    @Column(name = PlatformAbilityFields.SORT_COLUMN, type = ColumnType.INT, comment = "Sort order")
    private Integer sortOrder;
}
