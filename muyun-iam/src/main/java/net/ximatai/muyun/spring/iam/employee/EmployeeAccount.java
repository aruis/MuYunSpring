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
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;

@Getter
@Setter
@Table(name = "iam_employee_account", comment = "Employee account binding")
@CompositeIndex(columns = {"tenant_id", "employee_id", "user_id"}, unique = true)
@CompositeIndex(columns = {"tenant_id", "user_id"}, unique = true)
public class EmployeeAccount extends StandardEntity implements EnabledCapable {
    @Column(name = "employee_id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Employee id")
    private String employeeId;

    @Column(name = "user_id", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "User account id")
    private String userId;

    @Column(name = "primary_account", type = ColumnType.BOOLEAN, nullable = false, comment = "Primary account flag",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean primaryAccount = Boolean.FALSE;

    @Column(name = "enabled", type = ColumnType.BOOLEAN, nullable = false, comment = "Enabled flag",
            defaultVal = @Default(bool = TrueOrFalse.TRUE))
    private Boolean enabled = Boolean.TRUE;
}
